package lumi.insert.app.service.implement;
  
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import lumi.insert.app.aspect.annotation.ActivityLogger;

import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.StockCard;
import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.TransactionItem;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.StockMove;
import lumi.insert.app.core.entity.nondatabase.TransactionStatus;

import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.StockCardRepository;
import lumi.insert.app.core.repository.TransactionItemRepository;
import lumi.insert.app.core.repository.TransactionRepository;
import lumi.insert.app.core.repository.projection.ProductRefund;
import lumi.insert.app.core.repository.projection.ProductSale;

import lumi.insert.app.dto.request.ItemRefundRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.TransactionItemCreateRequest; 
import lumi.insert.app.dto.response.TransactionItemDelete;
import lumi.insert.app.dto.response.TransactionItemResponse;
import lumi.insert.app.dto.response.TransactionItemStatisticResponse;

import lumi.insert.app.exception.ForbiddenRequestException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.TransactionValidationException;
import lumi.insert.app.mapper.AllTransactionMapper;
import lumi.insert.app.service.TransactionItemService;
import lumi.insert.app.utils.generator.DateUtils;

/**
 * Implementation of {@link TransactionItemService} managing the lifecycle of line items within a transaction.
 * <p>
 * This service handles the items operations of a transaction, including:
 * <ul>
 * <li><b>Cart Operations:</b> Adding, updating, and removing items while a transaction is in {@code PENDING} status.</li>
 * <li><b>Stock Validation:</b> Real-time checking of product availability before cart insertion or quantity updates.</li>
 * <li><b>Recalculation:</b> Dynamically updating transaction totals (subtotal, grand total) as items change.</li> 
 * </ul>
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class TransactionItemServiceImpl implements TransactionItemService{

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    TransactionItemRepository transactionItemRepository;

    @Autowired
    StockCardRepository stockCardRepository;

    @Autowired
    AllTransactionMapper allTransactionMapper;

    @Autowired
    DateUtils datePicker;

    /**
     * Adds a product to a transaction cart.
     * <p>Increments the transaction's total item count and updates financial totals.</p>
     *
     * @param transactionId the target transaction UUID.
     * @param request       contains the product ID and requested quantity.
     * @return {@link TransactionItemResponse} representing the newly added line item.
     * @throws NotFoundEntityException        if the transaction or product does not exist.
     * @throws TransactionValidationException if the requested quantity exceeds available stock.
     */
    @Override
    @ActivityLogger(
        entityName = "transaction_items",
        action = ActivityAction.TRANSACTION_ITEM_CARTED,
        actionMessage = "New item carted to transaction cart"
    )
    public TransactionItemResponse createTransactionItem(UUID transactionId, TransactionItemCreateRequest request) {
        log.info("Creating transaction item for transactionId={} productId={} quantity={}", transactionId, request.getProductId(), request.getQuantity());
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new NotFoundEntityException("Transaction with ID " + transactionId + " was not found"));
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new NotFoundEntityException("Product with ID " + request.getProductId() + " was not found"));

        if(product.getStockQuantity().compareTo(request.getQuantity()) < 0) {
            log.debug("Transaction item creation failed due to insufficient stock productId={} requested={} available={}", request.getProductId(), request.getQuantity(), product.getStockQuantity());
            throw new TransactionValidationException("Product stocks with ID " + request.getProductId() + " doesn't meet buyer quantity, stock left: " + product.getStockQuantity());
        }
 
        TransactionItem transactionItem = TransactionItem.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .price(product.getSellPrice())
            .quantity(request.getQuantity())
            .productName(product.getName())
            .description(product.getName())
            .product(product)
            .transaction(transaction)
            .build();

        transactionItemRepository.save(transactionItem);
        
        BigDecimal addedSubtotal = transactionItem.getQuantity().multiply(transactionItem.getPrice());

        transaction.setTotalItems(transaction.getTotalItems() + 1);
        transaction.setSubTotal(transaction.getSubTotal().add(addedSubtotal));
        transaction.setGrandTotal(transaction.getSubTotal().subtract(transaction.getTotalDiscount()).add(transaction.getTotalFee()));
        
        TransactionItemResponse transactionItemResponseDto = allTransactionMapper.createTransactionItemResponseDto(transactionItem);
        return transactionItemResponseDto;
    }

    /**
     * Removes an item from the transaction cart.
     * <p>Only allowed if the transaction is still in {@code PENDING} status.</p>
     *
     * @param id the unique identifier of the transaction item to delete.
     * @return a DTO containing details of the deleted item for UI confirmation.
     * @throws ForbiddenRequestException if the transaction is already processed or completed.
     */
    @Override
    @ActivityLogger(
        entityName = "transaction_items",
        action = ActivityAction.TRANSACTION_ITEM_DELETED,
        actionMessage = "Item deleted from transaction cart"
    )
    public TransactionItemDelete deleteTransactionItem(UUID id) {
        TransactionItem transactionItem = transactionItemRepository.findById(id)
            .orElseThrow(() -> new NotFoundEntityException("Transaction Items with ID " + id + " was not found"));
        
        Transaction transaction = transactionItem.getTransaction();
 
        log.info("Deleting transaction item id={}", id);
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.debug("Delete failed, transaction not pending id={} status={}", transaction.getId(), transaction.getStatus());
            throw new ForbiddenRequestException("Unable to delete the item because Transaction Status is not PENDING(CART)");
        }
        BigDecimal deletedSubtotal = transactionItem.getQuantity().multiply(transactionItem.getPrice());

        transaction.setTotalItems(transaction.getTotalItems() - 1);
        transaction.setSubTotal(transaction.getSubTotal().subtract(deletedSubtotal));
        transaction.setGrandTotal(transaction.getGrandTotal().subtract(deletedSubtotal));

        transactionItemRepository.delete(transactionItem);
        TransactionItemDelete transactionItemDeleteResponseDto = allTransactionMapper.createTransactionItemDeleteResponseDto(transactionItem);
        return transactionItemDeleteResponseDto;
    }

    /**
     * Adjusts the quantity of an existing item in a pending cart.
     * <p>Triggers a full recalculation of the parent transaction's subtotal and grand total.</p>
     *
     * @param id       the transaction item UUID.
     * @param quantity the new desired quantity.
     * @return the updated {@link TransactionItemResponse}.
     * @throws TransactionValidationException if the new quantity exceeds current stock levels.
     */
    @Override
    @ActivityLogger(
        entityName = "transaction_items",
        action = ActivityAction.TRANSACTION_ITEM_UPDATED,
        actionMessage = "Item quantity updated"
    )
    public TransactionItemResponse updateTransactionItemQuantity(UUID id, BigDecimal quantity) {
        log.info("Updating transaction item quantity id={} newQuantity={}", id, quantity);
         TransactionItem transactionItem = transactionItemRepository.findById(id)
            .orElseThrow(() -> new NotFoundEntityException("Transaction Items with ID " + id + " was not found"));
        
        Transaction transaction = transactionItem.getTransaction();

        if(transaction.getStatus() != TransactionStatus.PENDING) throw new ForbiddenRequestException("Couldn't update the item because Transaction status is not PENDING(CART)");

        Product product = transactionItem.getProduct();

        if(product.getStockQuantity().compareTo(quantity) < 0) {
            log.debug("Update failed, insufficient stock productId={} requested={} available={}", product.getId(), quantity, product.getStockQuantity());
            throw new TransactionValidationException("Product stocks with ID " + product.getId() + " doesn't meet buyer quantity, stock left: " + product.getStockQuantity());
        }

        BigDecimal transactionItemOldSubTotal = transactionItem.getQuantity().multiply(transactionItem.getPrice());

        transactionItem.setPrice(product.getSellPrice());
        transactionItem.setQuantity(quantity);

        BigDecimal transactionItemNewSubTotal = transactionItem.getQuantity().multiply(transactionItem.getPrice());

        transaction.setSubTotal(transaction.getSubTotal().subtract(transactionItemOldSubTotal).add(transactionItemNewSubTotal));
        transaction.setGrandTotal(transaction.getGrandTotal().subtract(transactionItemOldSubTotal).add(transactionItemNewSubTotal));

        TransactionItemResponse transactionItemResponseDto = allTransactionMapper.createTransactionItemResponseDto(transactionItem);
        log.info("Transaction item quantity updated id={} newQuantity={}", id, quantity);
        return transactionItemResponseDto;
    }

    /**
     * Retrieves transaction items from a transaction.
     *
     * @param transactionId the transaction unique identifier.
     * @param request the pagination preset
     * @return slice of {@link TransactionItemResponse}.
     */
    @Override
    public Slice<TransactionItemResponse> getTransactionItemsByTransactionId(UUID transactionId, PaginationRequest request) {
        log.info("Retrieving items for transactionId={} page={} size={}", transactionId, request.getPage(), request.getSize());
        Sort sort = Sort.by("createdAt").ascending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize()).withSort(sort);

        Slice<TransactionItem> transactionItems = transactionItemRepository.findAllByTransactionId(transactionId, pageable);
        Slice<TransactionItemResponse> result = transactionItems.map(allTransactionMapper::createTransactionItemResponseDto);
        return result;
    }

    /**
     * Retrieves transaction items by transaction and product filter.
     *
     * @param transactionId the transaction unique identifier.
     * @param ProductId the product identifier.
     * @return slice of {@link TransactionItemResponse}.
     */
    @Override
    public Slice<TransactionItemResponse> getTransactionByTransactionIdAndProductId(UUID transactionId, Long ProductId) {
        log.info("Retrieving transaction items for transactionId={} productId={}", transactionId, ProductId);
        List<TransactionItem> searchedTransactionItem = transactionItemRepository.findByTransactionIdAndProductId(transactionId, ProductId);
        
        Slice<TransactionItem> slices = new SliceImpl<>(searchedTransactionItem);
        return slices.map(allTransactionMapper::createTransactionItemResponseDto); 
    }

    /**
     * Processes a refund for a specific product after a transaction has been processed or completed.
     * <p>
     * This method performs a multi-step adjustment:
     * <ul>
     * <li><b>Stock:</b> Increases physical stock and logs a {@code CUSTOMER_IN} movement.</li>
     * <li><b>Accounting:</b> Deducts from the customer's unpaid balance. If the refund amount 
     * exceeds the remaining debt, the surplus is added to {@code totalUnrefunded}.</li>
     * <li><b>Audit:</b> Creates a negative-quantity transaction item as a refund record.</li>
     * </ul>
     * </p>
     *
     * @param id      the parent transaction ID.
     * @param request contains product ID and quantity to be returned.
     * @return the refund record as a {@link TransactionItemResponse}.
     * @throws ForbiddenRequestException if the refund quantity exceeds originally purchased quantity.
     */
    @Override
    @ActivityLogger(
        entityName = "transaction_items",
        action = ActivityAction.TRANSACTION_ITEM_UPDATED,
        actionMessage = "Item quantity updated from transaction order"
    )
    public TransactionItemResponse refundTransactionItem(UUID id, ItemRefundRequest request) { 
        log.info("Refunding transaction item for transactionId={} productId={} quantity={}", id, request.getProductId(), request.getQuantity());

        List<TransactionItem> itemsWithMatchProduct = transactionItemRepository.findByTransactionIdAndProductId(id, request.getProductId());

        if(itemsWithMatchProduct.size() == 0) {
            log.debug("Refund failed, no matching transaction item for transactionId={} productId={}", id, request.getProductId());
            throw new NotFoundEntityException("Unable to find any transaction item with product id " + request.getProductId()); 
        }

        BigDecimal ttlRefundLeft = itemsWithMatchProduct.stream()
            .map(item -> item.getQuantity())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Check if request refund exceed real transaction quantity left
        if(ttlRefundLeft.compareTo(request.getQuantity()) < 0) {
            log.debug("Refund quantity exceeds purchased quantity transactionId={} productId={} requested={} available={}", id, request.getProductId(), request.getQuantity(), ttlRefundLeft);
            throw new ForbiddenRequestException("Refund quantity is more than actual bought, use valid quantity");
        }

        TransactionItem baseTransactionItem = itemsWithMatchProduct.getFirst();
        Transaction transaction = baseTransactionItem.getTransaction();
        
        if (transaction.getStatus() != TransactionStatus.PROCESS && transaction.getStatus() != TransactionStatus.COMPLETE ) throw new ForbiddenRequestException("Couldn't refund the item because Transaction Status is not PROCESS OR COMPLETE");

        Product product = baseTransactionItem.getProduct();;
        Customer customer = transaction.getCustomer(); 

        BigDecimal oldStock = product.getStockQuantity();

        product.setStockQuantity(product.getStockQuantity().add(request.getQuantity()));

        BigDecimal customerRefund = request.getQuantity().multiply(baseTransactionItem.getPrice());

        // Calculate transaction value. 
        // Check if total refund pay the unpaid or convert to unrefunded
        if(transaction.getTotalUnpaid().subtract(customerRefund).compareTo(BigDecimal.ZERO) < 0){
            customer.setTotalUnpaid(customer.getTotalUnpaid().subtract(transaction.getTotalUnpaid()));
            BigDecimal balanceLeft = customerRefund.subtract(transaction.getTotalUnpaid());
            customer.setTotalUnrefunded(customer.getTotalUnrefunded().add(balanceLeft));
            customer.setTotalPaid(customer.getTotalPaid().subtract(balanceLeft));

            transaction.setTotalUnrefunded(transaction.getTotalUnrefunded().add(balanceLeft));
            transaction.setTotalUnpaid(BigDecimal.ZERO);
            transaction.setTotalPaid(transaction.getTotalPaid().subtract(balanceLeft));
            transaction.setStatus(TransactionStatus.PROCESS);

        } else {
            transaction.setTotalUnpaid(transaction.getTotalUnpaid().subtract(customerRefund));
            customer.setTotalUnpaid(customer.getTotalUnpaid().subtract(customerRefund));
        }

        //reverse
        TransactionItem refundTransactionItem = TransactionItem.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .price(baseTransactionItem.getPrice())
            .quantity(request.getQuantity().negate())
            .description("REFUND: " + product.getName())
            .product(product)
            .productName(product.getName())
            .transaction(transaction)
            .build();

        StockCard stockCard = StockCard.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .referenceId(refundTransactionItem.getId())
            .product(product)
            .productName(product.getName())
            .quantity(request.getQuantity())
            .oldStock(oldStock)
            .newStock(product.getStockQuantity())
            .type(StockMove.CUSTOMER_IN)
            .oldPrice(product.getBasePrice())
            .newPrice(product.getBasePrice())
            .description("Transaction Cancelled, Product refunded. Status: CUSTOMER_IN(IN)")
            .build();

        stockCardRepository.save(stockCard); 
  
        TransactionItem savedRefundTransactionItem = transactionItemRepository.save(refundTransactionItem);
        TransactionItemResponse transactionItemResponseDto = allTransactionMapper.createTransactionItemResponseDto(savedRefundTransactionItem);
        log.info("Refund transaction item created itemId={} transactionId={}", savedRefundTransactionItem.getId(), id);
        return transactionItemResponseDto;
    }

    /**
     * Retrieves detailed transaction item by identifier.
     *
     * @param transactionId the transaction item unique identifier. 
     * @return {@link TransactionItemResponse}.
     */
    @Override
    public TransactionItemResponse getTransactionItem(UUID id) {
        log.info("Retrieving transaction item id={}", id);
        TransactionItem transactionItem = transactionItemRepository.findById(id)
            .orElseThrow(() -> new NotFoundEntityException("Transaction Items with ID " + id + " was not found"));

        TransactionItemResponse transactionItemResponseDto = allTransactionMapper.createTransactionItemResponseDto(transactionItem);
        return transactionItemResponseDto;
    }

    /**
     * Retrieves product statistic base on transaction item mapping.
     *
     * @param startDate transaction fetch start date. 
     * @param endDate transaction fetch end date.
     * @return {@link TransactionItemStatisticResponse}.
     */
    @Override
    @Cacheable(
        value = "products-stats",
        key = "#startDate.getDayOfMonth() + '_' + #endDate.getDayOfMonth()",
        condition = "#startDate.getHour() == 0 && #startDate.getMinute() == 0 && #startDate.getSecond() == 0 && #endDate.getHour() == 23 && #endDate.getMinute() == 59 && #endDate.getSecond() == 59"
    )
    public TransactionItemStatisticResponse getTransactionItemStats(LocalDateTime startDate, LocalDateTime endDate) { 
        log.info("Gathering transaction item stats from {} to {}", startDate, endDate);
        List<ProductSale> productTopSales = transactionItemRepository.getProductTopSales(startDate, endDate);
        List<ProductRefund> productTopRefunds  = transactionItemRepository.getProductTopRefund(startDate, endDate);
        return TransactionItemStatisticResponse.builder().productSales(productTopSales).productRefunds(productTopRefunds).build();
    }

}
