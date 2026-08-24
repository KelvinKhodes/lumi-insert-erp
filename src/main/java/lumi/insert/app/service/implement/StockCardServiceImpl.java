package lumi.insert.app.service.implement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.StockCard;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.StockMove;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.StockCardRepository;
import lumi.insert.app.core.repository.TransactionItemRepository;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.StockCardCreateRequest;
import lumi.insert.app.dto.request.StockCardGetByFilter;
import lumi.insert.app.dto.response.StockCardResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.TransactionValidationException;
import lumi.insert.app.mapper.StockCardMapper;
import lumi.insert.app.service.StockCardService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

/**
 * Implementation of {@link StockCardService} providing a detailed audit trail for inventory movements.
 * <p>
 * This service is responsible for recording every stock change (Stock Card) and ensuring 
 * the physical stock in {@link Product} remains consistent. It enforces strict validation 
 * on stock direction (In/Out) and prevents negative inventory balances.
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class StockCardServiceImpl implements StockCardService{

    @Autowired
    StockCardRepository stockCardRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    TransactionItemRepository transactionItemRepository;

    @Autowired
    StockCardMapper stockCardMapper;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    /**
     * Records a new stock movement and updates the current product's stock level.
     * <p>
     * Logic includes:
     * <ul>
     * <li>Validation of quantity polarity based on {@link StockMove} type (e.g., CUSTOMER_OUT must be negative).</li>
     * <li>Reference integrity check for customer-related transactions.</li>
     * <li>Prevention of stock levels falling below zero.</li>
     * <li>Snapshot recording of prices and stock levels (old vs new) for audit purposes.</li>
     * </ul>
     * </p>
     *
     * @param request the details of the stock movement.
     * @return the created {@link StockCardResponse} containing the movement snapshot.
     * @throws TransactionValidationException if quantity polarity is invalid or stock is insufficient.
     * @throws NotFoundEntityException        if the product or transaction reference is missing.
     */
    @Override
    @ActivityLogger(
        entityName = "stock_cards",
        action = ActivityAction.STOCK_ADJUSTMENT,
        actionMessage = "Product stock adjustment"
    )
    public StockCardResponse createStockCard(StockCardCreateRequest request) {
        log.info("Creating stock card for productId={}, referenceId={}, type={}", request.getProductId(), request.getReferenceId(), request.getType());
        if((request.getType().equals(StockMove.CUSTOMER_IN.toString()) || request.getType().equals(StockMove.SUPPLIER_IN.toString()) ||
            request.getType().equals(StockMove.REPAIRED.toString())) && request.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                log.debug("Invalid stock card quantity for IN type, quantity={}", request.getQuantity());
                throw new TransactionValidationException("Stock 'IN' type should be positive quantity");
        }

        if((request.getType().equals(StockMove.CUSTOMER_OUT.toString()) || request.getType().equals(StockMove.SUPPLIER_OUT.toString()) ||
            request.getType().equals(StockMove.DEFECT.toString())) && request.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                log.debug("Invalid stock card quantity for OUT type, quantity={}", request.getQuantity());
                throw new TransactionValidationException("Stock 'OUT' type should be negative quantity");
        }

        if((request.getType().equals(StockMove.CUSTOMER_IN.toString()) || request.getType().equals(StockMove.CUSTOMER_OUT.toString())) && !transactionItemRepository.existsById(request.getReferenceId())) {
            log.debug("Stock card creation failed, transaction item not found referenceId={}", request.getReferenceId());
            throw new NotFoundEntityException("Transaction Items with ID " + request.getReferenceId() + " was not found");
        }

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new NotFoundEntityException("Product with ID " + request.getProductId() + " was not found"));

        BigDecimal oldStock = product.getStockQuantity();

        product.setStockQuantity(oldStock.add(request.getQuantity()));
 
        if(product.getStockQuantity().compareTo(BigDecimal.ZERO) < 0) throw new TransactionValidationException("Product stocks with ID " + request.getProductId() + " doesn't meet buyer quantity, stock left: " + oldStock);

        StockCard stockCard = StockCard.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .referenceId(request.getReferenceId())
            .product(product)
            .productName(product.getName())
            .quantity(request.getQuantity())
            .oldStock(oldStock)
            .newStock(product.getStockQuantity())
            .type(StockMove.valueOf(request.getType()))
            .oldPrice(product.getBasePrice())
            .newPrice(product.getBasePrice())
            .description(request.getDescription())
            .build();

        StockCard savedStockCard = stockCardRepository.save(stockCard);
        
        StockCardResponse stockCardResponse = stockCardMapper.createDtoResponseFromStockCard(savedStockCard); 
        log.info("Stock card created stockCardId={} productId={}", savedStockCard.getId(), savedStockCard.getProduct().getId());
        return stockCardResponse;
    }

    /**
     * Retrieves a specific stock card record by its unique identifier.
     *
     * @param id the unique UUID of the stock card.
     * @return the found {@link StockCardResponse}.
     * @throws NotFoundEntityException if the stock card record does not exist.
     */
    @Override
    public StockCardResponse getStockCard(UUID id) {
        log.info("Retrieving stock card id={}", id);
        StockCard stockCard = stockCardRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Stock card not found id={}", id);
                return new NotFoundEntityException("StockCard with ID " + id + " was not found");
            });

        return stockCardMapper.createDtoResponseFromStockCard(stockCard);
    }

    /**
     * Retrieves a slice of stock cards using index-based (keyset) pagination.
     * <p>Optimized for large audit logs by using the {@code lastId} to avoid high offset performance hits.</p>
     *
     * @param lastId  the last UUID seen in the previous page (null for the first page).
     * @param request pagination parameters (size, sort).
     * @return a {@link Slice} of stock card records.
     */
    @Override
    public Slice<StockCardResponse> getStockCards(UUID lastId, PaginationRequest request) {
        if(lastId != null) request.setPage(0);
        Pageable pageable = jpaSpecGenerator.pageable(request);

        Slice<StockCardResponse> slices = stockCardRepository.findByIndexPagination(LocalDateTime.of(1900, 10, 10, 10, 10), LocalDateTime.of(3000, 10, 10, 10, 10), lastId, pageable);

        return slices;
    }

    /**
     * Searches and filters stock card records using dynamic criteria.
     * <p>Supports filtering by date range, product, and movement type via JPA Specifications.</p>
     *
     * @param request the filter and pagination criteria.
     * @return a filtered {@link Slice} of {@link StockCardResponse}.
     */
    @Override
    public Slice<StockCardResponse> searchStockCards(StockCardGetByFilter request) {
        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<StockCard> stockCardSpecification = jpaSpecGenerator.stockCardSpecification(request);

        Slice<StockCard> slices = stockCardRepository.findAll(stockCardSpecification, pageable);

        return slices.map(stockCardMapper::createDtoResponseFromStockCard);
    }
    
}
