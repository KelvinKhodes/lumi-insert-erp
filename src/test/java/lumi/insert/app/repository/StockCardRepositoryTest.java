package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List; 

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.StockCard;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.entity.nondatabase.StockMove;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.StockCardRepository;
import lumi.insert.app.dto.request.StockCardGetByFilter;
import lumi.insert.app.dto.response.StockCardResponse;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import({JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
@ActiveProfiles("test")

public class StockCardRepositoryTest  extends TestContainerTest {
    
    @Autowired
    StockCardRepository stockCardRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    Product product;

    @BeforeEach
    void setup(){
        productRepository.deleteAll();
        
        EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("Test Username")
        .role(EmployeeRole.CASHIER)
        .ipAddress("t.e.s.t")
        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        product = productRepository.saveAndFlush(Product.builder().name("Shoes").basePrice(BigDecimal.valueOf(1000L)).sellPrice(BigDecimal.valueOf(1100L)).stockQuantity(BigDecimal.valueOf(10L)).build());
    }

    @Test
    void findByIndexPagination_foundEntity_returnSliceDTO(){
        StockCard stockCard1 = StockCard.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .referenceId(UuidCreator.getTimeOrderedEpochFast())
        .product(product)
        .productName(product.getName())
        .quantity(BigDecimal.valueOf(-5L))
        .oldStock(BigDecimal.valueOf(10L))
        .newStock(BigDecimal.valueOf(5L))
        .type(StockMove.SALE)
        .oldPrice(BigDecimal.valueOf(1000L))
        .newPrice(BigDecimal.valueOf(1000L))
        .build();

        StockCard stockCard2 = StockCard.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .referenceId(UuidCreator.getTimeOrderedEpochFast())
        .product(product)
        .productName(product.getName())
        .quantity(BigDecimal.valueOf(-5L))
        .oldStock(BigDecimal.valueOf(10L))
        .newStock(BigDecimal.valueOf(5L))
        .type(StockMove.SALE)
        .oldPrice(BigDecimal.valueOf(1000L))
        .newPrice(BigDecimal.valueOf(1000L))
        .build();

        stockCardRepository.saveAndFlush(stockCard1);
        StockCard saveAndFlush2 = stockCardRepository.saveAndFlush(stockCard2);;

        Slice<StockCardResponse> byIndexPagination = stockCardRepository.findByIndexPagination(LocalDateTime.now().minusDays(1), LocalDateTime.now(), null, PageRequest.of(0, 2));
        assertEquals(2, byIndexPagination.getNumberOfElements());
        assertEquals(saveAndFlush2.getId(), byIndexPagination.getContent().getLast().id());
    }

    @Test
    void findByIndexPagination_foundEntityFilterLastId_returnSliceDTO(){
        StockCard stockCard1 = StockCard.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .referenceId(UuidCreator.getTimeOrderedEpochFast())
        .product(product)
        .productName(product.getName())
        .quantity(BigDecimal.valueOf(-5L))
        .oldStock(BigDecimal.valueOf(10L))
        .newStock(BigDecimal.valueOf(5L))
        .type(StockMove.SALE)
        .oldPrice(BigDecimal.valueOf(1000L))
        .newPrice(BigDecimal.valueOf(1000L))
        .build();

        StockCard stockCard2 = StockCard.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .referenceId(UuidCreator.getTimeOrderedEpochFast())
        .product(product)
        .productName(product.getName())
        .quantity(BigDecimal.valueOf(-5L))
        .oldStock(BigDecimal.valueOf(10L))
        .newStock(BigDecimal.valueOf(5L))
        .type(StockMove.SALE)
        .oldPrice(BigDecimal.valueOf(1000L))
        .newPrice(BigDecimal.valueOf(1000L))
        .build();

        StockCard saveAndFlush = stockCardRepository.saveAndFlush(stockCard1);
        StockCard saveAndFlush2 = stockCardRepository.saveAndFlush(stockCard2);;

        Slice<StockCardResponse> byIndexPagination = stockCardRepository.findByIndexPagination(LocalDateTime.now().minusDays(1), LocalDateTime.now(), saveAndFlush.getId(), PageRequest.of(0, 2));
        assertEquals(1, byIndexPagination.getNumberOfElements());
        assertEquals(saveAndFlush2.getId(), byIndexPagination.getContent().getLast().id());
    }

    @Test
    void findAllSpecification_foundEntityFilterLastId_returnSliceDTO(){
        StockCard stockCard1 = StockCard.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .referenceId(UuidCreator.getTimeOrderedEpochFast())
        .product(product)
        .productName(product.getName())
        .quantity(BigDecimal.valueOf(-5L))
        .oldStock(BigDecimal.valueOf(10L))
        .newStock(BigDecimal.valueOf(5L))
        .type(StockMove.PURCHASE)
        .oldPrice(BigDecimal.valueOf(1000L))
        .newPrice(BigDecimal.valueOf(1000L))
        .build();

        StockCard stockCard2 = StockCard.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .referenceId(UuidCreator.getTimeOrderedEpochFast())
        .product(product)
        .productName(product.getName())
        .quantity(BigDecimal.valueOf(-5L))
        .oldStock(BigDecimal.valueOf(10L))
        .newStock(BigDecimal.valueOf(5L))
        .type(StockMove.SALE)
        .oldPrice(BigDecimal.valueOf(1000L))
        .newPrice(BigDecimal.valueOf(1000L))
        .build();

        List<StockCard> saveAllAndFlush = stockCardRepository.saveAllAndFlush(List.of(stockCard1, stockCard2));;

        StockCardGetByFilter request = StockCardGetByFilter.builder()
        .type(StockMove.PURCHASE)
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);
        Specification<StockCard> stockCardSpecification = jpaSpecGenerator.stockCardSpecification(request);

        Slice<StockCard> slices = stockCardRepository.findAll(stockCardSpecification, pageable);;
        assertEquals(1, slices.getNumberOfElements());
        assertTrue(slices.isLast());
        assertEquals(saveAllAndFlush.getFirst().getId(), slices.getContent().getLast().getId());
    }


}
