package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List; 
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.entity.Supply;
import lumi.insert.app.core.entity.SupplyItem;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.core.repository.SupplyItemRepository;
import lumi.insert.app.core.repository.SupplyRepository;
import lumi.insert.app.utils.forTesting.ProductUtils;
import lumi.insert.app.utils.generator.InvoiceGenerator;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import({InvoiceGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class })
@ActiveProfiles("test")
public class SupplyItemRepositoryTest  extends TestContainerTest {

    @Autowired
    SupplyRepository supplyRepository;
    
    @Autowired
    SupplyItemRepository supplyItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired 
    InvoiceGenerator invoiceGenerator;

    @Autowired
    SupplierRepository supplierRepository;

    Supplier supplier;

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
        
        supplier = supplierRepository.save(Supplier.builder().id(UuidCreator.getTimeOrderedEpochFast()).name("TESTES").contact("TESTE123").build());
    }

    @Test
    @DisplayName("Should add supply_items entity to database when base field < invoiceId required is valid")
    public void createSupplyItems_baseField_returnSavedEntity(){
        String invoiceId = invoiceGenerator.generate();

        Supply supply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .supplier(supplier)
        .supplierName(supplier.getName())
        .build();

       Supply savedSupply = supplyRepository.save(supply);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.save(mockCategorizedProduct);

       SupplyItem supplyItem = SupplyItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .supply(savedSupply)
       .product(savedProduct)
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

        SupplyItem saveAndFlushTrxItems = supplyItemRepository.saveAndFlush(supplyItem);

        assertNotNull(saveAndFlushTrxItems.getCreatedAt());
        assertEquals(savedProduct.getId(), saveAndFlushTrxItems.getProduct().getId());
        assertEquals(savedSupply.getId(), saveAndFlushTrxItems.getSupply().getId());
    }

    @Test
    @DisplayName("Should return supply_items entity when supply and product id is valid")
    public void findBySupplyIdAndProductId_validId_returnEntity(){
        String invoiceId = invoiceGenerator.generate();

        Supply supply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .supplier(supplier)
        .supplierName(supplier.getName())
        .build();

       Supply savedSupply = supplyRepository.save(supply);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.save(mockCategorizedProduct);

       SupplyItem supplyItem = SupplyItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .supply(savedSupply)
       .product(savedProduct)
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

        supplyItemRepository.saveAndFlush(supplyItem);

        List<SupplyItem> searchedItem = supplyItemRepository.findBySupplyIdAndProductId(savedSupply.getId(), savedProduct.getId());

        assertEquals(1, searchedItem.size());
        assertEquals(savedProduct.getId(), searchedItem.getFirst().getProduct().getId());
        assertEquals(savedSupply.getId(), searchedItem.getFirst().getSupply().getId());
    }

    @Test
    @DisplayName("Should return Optional Empty entity when supply and product id is not valid")
    public void findBySupplyIdAndProductId_invalidId_returnOptionalEmptyEntity(){
        List<SupplyItem> searchedItem = supplyItemRepository.findBySupplyIdAndProductId(UUID.randomUUID(), 15L);

        assertTrue(searchedItem.isEmpty());
    }

    @Test
    @DisplayName("Should return Slice of supply_items entity")
    public void findAllBySupplyId_validRequest_returnPageableEntity(){
        String invoiceId = invoiceGenerator.generate();

            Supply supply = Supply.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceId)
            .supplier(supplier)
            .supplierName(supplier.getName())
            .build();

            Supply savedSupply = supplyRepository.save(supply);

            
        for (int i = 0; i < 3; i++) {
            Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
            mockCategorizedProduct.setCategory(null);
            mockCategorizedProduct.setId(null);
            mockCategorizedProduct.setName(mockCategorizedProduct.getName() + i);
            Product savedProduct = productRepository.save(mockCategorizedProduct);

            SupplyItem supplyItem = SupplyItem.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .supply(savedSupply)
            .product(savedProduct)
            .price(savedProduct.getBasePrice())
            .quantity(savedProduct.getStockQuantity())
            .build();

            supplyItemRepository.saveAndFlush(supplyItem);
        }
        PageRequest pageable = PageRequest.of(0, 2, Sort.by("createdAt").ascending());
        Slice<SupplyItem> searchedItem = supplyItemRepository.findAllBySupplyId(savedSupply.getId(), pageable);

        assertEquals(2, searchedItem.getNumberOfElements());
        assertTrue(searchedItem.hasNext());
        assertEquals("Product1", searchedItem.getContent().getLast().getProduct().getName());
    }

    @Test
    @DisplayName("Should return Optional Empty entity when supply and product id is not valid")
    public void findAllBySupplyId_invalidId_returnOptionalEmptyEntity(){
        Slice<SupplyItem> searchedItem = supplyItemRepository.findAllBySupplyId(UUID.randomUUID(), PageRequest.of(0, 2, Sort.by("createdAt").ascending()));

        assertTrue(searchedItem.isEmpty());
    }

}
