package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Category;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.CategoryRepository;
import lumi.insert.app.core.repository.ProductRepository;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@ActiveProfiles("test")
@Slf4j
@Import({AuditorAwareImpl.class, SimpleCacheManager.class})
public class CategoryRepositoryTest extends TestContainerTest {
    
    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    EntityManager entityManager;

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
    public void testSaveCategory() {
        Category dumpCategory = Category.builder()
        .name("Shoes")
        .build();

        Category savedCategory = categoryRepository.save(dumpCategory);

        Category foundCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();

        assertEquals("Shoes", foundCategory.getName());
    }

    @Test
    public void testFindProductFromCategory() {
        Category dumpCategory = Category.builder()
        .name("Shoes")
        .build();

        Category savedCategory = categoryRepository.save(dumpCategory);

        Product dumpCategoriedProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .category(savedCategory)
        .build();

        Product dumpProduct = Product.builder()
        .name("PUMA Running Pro")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        Product savedCategoriedProduct = productRepository.save(dumpCategoriedProduct);
        Product savedProduct = productRepository.save(dumpProduct);

        assertNotSame(savedCategoriedProduct, savedProduct);

        categoryRepository.flush();
        entityManager.clear();
        
        Category foundCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();
        log.info("Found category: {}", foundCategory);
        List<Product> product = foundCategory.getProduct();
        assertEquals(1, product.size());
        assertEquals("NIKE Jordan Low 3", product.getFirst().getName());
    }

    @Test
    public void testExistsByName() {
        Category dumpCategory = Category.builder()
        .name("Shoes")
        .build();

        categoryRepository.save(dumpCategory);

        boolean exists = categoryRepository.existsByName("Shoes");
        assertEquals(true, exists);

        boolean notExists = categoryRepository.existsByName("Electronics");
        assertEquals(false, notExists);
    }

    @Test
    public void testFindAllByIsActiveTrue_shouldReturnOnlyActiveCategory(){
        Category dumpCategory = Category.builder()
        .name("Shoes Active")
        .build();

        Category dumpCategoryInactive = Category.builder()
        .name("Shoes Inactive")
        .isActive(false)
        .build();

        categoryRepository.saveAll(List.of(dumpCategory, dumpCategoryInactive));

        Slice<Category> allByIsActiveTrue = categoryRepository.findAllByIsActiveTrue(PageRequest.of(0, 5));

        assertEquals(1, allByIsActiveTrue.getNumberOfElements());
    }

    @Test
    public void testFindAllByIsInactiveTrue_shouldReturnOnlyInactiveCategory(){
        Category dumpCategory = Category.builder()
        .name("Shoes Active")
        .build();

        Category dumpCategoryInactive = Category.builder()
        .name("Shoes Inactive")
        .isActive(false)
        .build();

        Category dumpCategoryInactive2 = Category.builder()
        .name("Shoes Inactive 2")
        .isActive(false)
        .build();

        categoryRepository.saveAll(List.of(dumpCategory, dumpCategoryInactive, dumpCategoryInactive2));

        Slice<Category> allByIsActiveTrue = categoryRepository.findAllByIsActiveFalse(PageRequest.of(0, 5));

        assertEquals(2, allByIsActiveTrue.getNumberOfElements());
    }
}
