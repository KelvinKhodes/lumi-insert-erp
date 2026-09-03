package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; 

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice; 
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.projection.ProductOutOfStock;
import lumi.insert.app.core.repository.projection.ProductRefreshProjection;
import lumi.insert.app.dto.request.ProductGetByFilter;
import lumi.insert.app.dto.response.ProductName;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Slf4j 
@ActiveProfiles("test")
@Import({JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
public class ProductRepositoryTest  extends TestContainerTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    @BeforeEach
    void setup(){
        EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("Test Username")
        .role(EmployeeRole.CASHIER)
        .ipAddress("t.e.s.t")
        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void testSaveProduct() {
        Product dumpProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        Product savedProduct = productRepository.save(dumpProduct);

        Optional<Product> byId = productRepository.findById(savedProduct.getId());

        if( byId.isPresent() ) {
            Product foundProduct = byId.get();
            assertEquals("NIKE Jordan Low 3", foundProduct.getName());
            assertTrue(BigDecimal.valueOf(10000L).compareTo(foundProduct.getBasePrice()) == 0);
            assertTrue(BigDecimal.valueOf(12000L).compareTo(foundProduct.getSellPrice()) == 0);
            assertTrue(BigDecimal.valueOf(50L).compareTo(foundProduct.getStockQuantity()) == 0);
            assertTrue(BigDecimal.valueOf(5L).compareTo(foundProduct.getStockMinimum()) == 0);
        } else {
            Assertions.fail("Product not found");
        }
    }

    @Test
    public void testGetStockById() {
        Product dumpProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        Product savedProduct = productRepository.save(dumpProduct);

        Optional<BigDecimal> stockProjection = productRepository.getStockById(savedProduct.getId());

        assertTrue(BigDecimal.valueOf(50L).compareTo(stockProjection.get()) == 0);
    }

    @Test
    public void testFindAllByName() {
        Product dumpProduct1 = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        Product dumpProduct2 = Product.builder()
        .name("NIKE Kyrie 5")
        .basePrice(BigDecimal.valueOf(11000L))
        .sellPrice(BigDecimal.valueOf(13000L))
        .stockQuantity(BigDecimal.valueOf(30L))
        .stockMinimum(BigDecimal.valueOf(3L))
        .build();

        Product dumpProductInactive = Product.builder()
        .name("NIKE Jordan Low Inactive")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .isActive(false)
        .build();

        productRepository.save(dumpProduct1);
        productRepository.save(dumpProduct2);
        productRepository.save(dumpProductInactive);

        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").ascending()); 
        Slice<ProductName> products = productRepository.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter("Jordan", 0L, pageable);
        Slice<ProductName> productsNike = productRepository.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter("NIKE", 0L, pageable);
        Slice<ProductName> productsSomething = productRepository.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter("Something", 0L,  pageable);

        assertEquals(1, products.getNumberOfElements());
        assertEquals(2, productsNike.getNumberOfElements());
        assertFalse(productsNike.hasNext());
        assertTrue(productsSomething.isEmpty());
    }

    @Test
    public void testFindAllByName_lastId_foundEntity() {
        Product dumpProduct1 = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        Product dumpProduct2 = Product.builder()
        .name("NIKE Kyrie 5")
        .basePrice(BigDecimal.valueOf(11000L))
        .sellPrice(BigDecimal.valueOf(13000L))
        .stockQuantity(BigDecimal.valueOf(30L))
        .stockMinimum(BigDecimal.valueOf(3L))
        .build(); 

        productRepository.save(dumpProduct1);
        productRepository.save(dumpProduct2); 

        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").ascending()); 
        Slice<ProductName> products = productRepository.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter("Jordan", 99999L, pageable);
        Slice<ProductName> productsNike = productRepository.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter("NIKE", 0L, pageable);
        Slice<ProductName> productsSomething = productRepository.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter("Something", 0L,  pageable);

        assertEquals(0, products.getNumberOfElements());
        assertEquals(2, productsNike.getNumberOfElements());
        assertFalse(productsNike.hasNext());
        assertTrue(productsSomething.isEmpty());
    }

    @Test
    public void testFindAllPagination() {
        for ( int i = 1; i <= 12; i++ ) {
            Product dumpProduct = Product.builder()
            .name("Product " + i)
            .basePrice(BigDecimal.valueOf(1000L * i))
            .sellPrice(BigDecimal.valueOf(1200L * i))
            .stockQuantity(BigDecimal.valueOf(10L * i))
            .stockMinimum(BigDecimal.valueOf(1L * i))
            .build();

            productRepository.save(dumpProduct);
        }
        Pageable pageable = PageRequest.of(0, 5, Sort.by("sellPrice").ascending());
        Slice<Product> products = productRepository.findAllBy(pageable);
        assertTrue(products.hasNext());
        assertEquals(5, products.getNumberOfElements());
        List<Product> productsSet = products.getContent();
        System.out.println(productsSet);

        assertEquals("Product 1", productsSet.getFirst().getName());
        assertEquals("Product 5", productsSet.getLast().getName());
        
        if( products.hasNext() ) {
            pageable = products.nextPageable();
            products = productRepository.findAllBy(pageable);
            assertTrue(products.hasNext());
            assertEquals(5, products.getNumberOfElements());
            productsSet = products.getContent();

            assertEquals("Product 6", productsSet.getFirst().getName());
            assertEquals("Product 10", productsSet.getLast().getName());

            if( products.hasNext() ) {
                pageable = products.nextPageable();
                products = productRepository.findAllBy(pageable);
                assertFalse(products.hasNext());
                assertEquals(2, products.getNumberOfElements());
                productsSet = products.getContent();

                assertEquals("Product 11", productsSet.getFirst().getName());
                assertEquals("Product 12", productsSet.getLast().getName());
            }
        }
    }

    @Test
    public void testExistsByName() {
        Product dumpProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        productRepository.save(dumpProduct);

        Boolean exists = productRepository.existsByName("NIKE Jordan Low 3");
        assertTrue(exists);

        Boolean notExists = productRepository.existsByName("Adidas Superstar");
        assertFalse(notExists);
    }

    @Test
    public void testGetStockByIdNotFound() {
        Optional<BigDecimal> stockProjection = productRepository.getStockById(999L);
        assertEquals(null, stockProjection.orElse(null));
    }

    @Test
    public void testGetProductCriteria(){
        for ( int i = 1; i <= 12; i++ ) {
            Product dumpProduct = Product.builder()
            .name("Product " + i)
            .basePrice(BigDecimal.valueOf(1000L * i))
            .sellPrice(BigDecimal.valueOf(1200L * i))
            .stockQuantity(BigDecimal.valueOf(10L * i))
            .stockMinimum(BigDecimal.valueOf(1L * i))
            .build();

            productRepository.save(dumpProduct);
        }
        ProductGetByFilter request = ProductGetByFilter.builder()
        .size(5)
        .sortBy("sellPrice")
        .sortDirection("ASC")
        .name("pro")
        .minPrice(BigDecimal.ZERO)
        .maxPrice(BigDecimal.valueOf(5000L))
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);
        Specification<Product> specification = jpaSpecGenerator.productSpecification(request);
 
        Slice<Product> result = productRepository.findAll(specification, pageable);

        System.out.println(result.getContent());
        assertEquals(4, result.getNumberOfElements());
        assertTrue(BigDecimal.valueOf(4800L).compareTo(result.getContent().getLast().getSellPrice()) == 0);
    }

    @Test
    @DisplayName("Should return List of ProductRefreshProjection<id, sPrice, stockQ>")
    public void searchIdUpdatedAtMoreThan_validListId_returnListProductRefreshProjection(){
        LocalDateTime time = LocalDateTime.now();
        List<Long> ids = new ArrayList<>();
        for ( int i = 1; i <= 2; i++ ) {
            Product dumpProduct = Product.builder()
            .name("Product " + i)
            .basePrice(BigDecimal.valueOf(1000L * i))
            .sellPrice(BigDecimal.valueOf(1200L * i))
            .stockQuantity(BigDecimal.valueOf(10L * i))
            .stockMinimum(BigDecimal.valueOf(1L * i))
            .build();

            Product saved = productRepository.save(dumpProduct);
            ids.add(saved.getId());
        }
       List<ProductRefreshProjection> searchIdUpdatedAtMoreThan = productRepository.searchIdUpdatedAtMoreThan(ids, time);
       log.info("x{}", searchIdUpdatedAtMoreThan.getLast());
       assertEquals(2, searchIdUpdatedAtMoreThan.size());
       assertTrue(BigDecimal.valueOf(2400L).compareTo(searchIdUpdatedAtMoreThan.getLast().sellPrice()) == 0);
    }
    
    @Test
    @DisplayName("Should return List of ProductRefreshProjection<id, sPrice, stockQ>")
    public void searchIdUpdatedAtMoreThan_timeIsNewer_return0ProductRefreshProjection(){ 
        List<Long> ids = new ArrayList<>();
        for ( int i = 1; i <= 2; i++ ) {
            Product dumpProduct = Product.builder()
            .name("Product " + i)
            .basePrice(BigDecimal.valueOf(1000L * i))
            .sellPrice(BigDecimal.valueOf(1200L * i))
            .stockQuantity(BigDecimal.valueOf(10L * i))
            .stockMinimum(BigDecimal.valueOf(1L * i))
            .build();

            Product saved = productRepository.save(dumpProduct);
            ids.add(saved.getId());
        }
       List<ProductRefreshProjection> searchIdUpdatedAtMoreThan = productRepository.searchIdUpdatedAtMoreThan(ids, LocalDateTime.now().plusDays(1));
       assertEquals(0, searchIdUpdatedAtMoreThan.size()); 
    }

    @Test
    @DisplayName("Should return List of Product Entity")
    public void searchProductUpdatedAtMoreThan_validListId_returnListProduct(){
        LocalDateTime time = LocalDateTime.now();
        List<Long> ids = new ArrayList<>();
        for ( int i = 1; i <= 2; i++ ) {
            Product dumpProduct = Product.builder()
            .name("Product " + i)
            .basePrice(BigDecimal.valueOf(1000L * i))
            .sellPrice(BigDecimal.valueOf(1200L * i))
            .stockQuantity(BigDecimal.valueOf(10L * i))
            .stockMinimum(BigDecimal.valueOf(1L * i))
            .build();

            Product saved = productRepository.save(dumpProduct);
            ids.add(saved.getId());
        }
       List<Product> searchIdUpdatedAtMoreThan = productRepository.searchProductUpdatedAtMoreThan(ids, time); 
       assertEquals(2, searchIdUpdatedAtMoreThan.size());
       assertTrue(BigDecimal.valueOf(2400L).compareTo(searchIdUpdatedAtMoreThan.getLast().getSellPrice()) == 0);
    
    }

    @Test
    public void getOutOfStockProducts_foundEntity_returnProjection() {
        Product dumpProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(2L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        productRepository.save(dumpProduct);

        List<ProductOutOfStock> exists = productRepository.findAllOutOfStockProduct();
        assertEquals(1, exists.size());
        assertEquals(dumpProduct.getStockQuantity().longValue(), exists.getFirst().stockQuantity().longValue());
 
    }

}
