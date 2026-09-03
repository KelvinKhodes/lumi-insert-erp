package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime; 
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest; 
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;  
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice; 
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.entity.Supply;
import lumi.insert.app.core.entity.SupplyItem;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.entity.nondatabase.SupplyStatus;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.core.repository.SupplyItemRepository;
import lumi.insert.app.core.repository.SupplyRepository;
import lumi.insert.app.dto.request.SupplyGetByFilter;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.utils.forTesting.ProductUtils;
import lumi.insert.app.utils.generator.InvoiceGenerator;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Slf4j
@Import({JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
@ActiveProfiles("test")
public class SupplyRepositoryTest  extends TestContainerTest {
    
    @Autowired
    SupplyRepository supplyRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    InvoiceGenerator invoiceGenerator = new InvoiceGenerator();

    Supplier setupSupplier;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplyItemRepository supplyItemRepository;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    public void setup(){
        EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("Test Username")
        .role(EmployeeRole.CASHIER)
        .ipAddress("t.e.s.t")
        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Supplier supplier1 = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("Supplier 1")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .build();

        setupSupplier = supplierRepository.saveAndFlush(supplier1);
    }

    @Test
    @DisplayName("Should add supply entity to database when base field < invoiceId required is valid")
    public void createSupply_baseField_returnSavedEntity(){
        String invoiceId = invoiceGenerator.generate();

        Supply supply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .supplier(setupSupplier)
        .supplierName(setupSupplier.getName())
        .build();

        Supply savedSupply = supplyRepository.save(supply);

        assertTrue(BigDecimal.ZERO.compareTo(savedSupply.getGrandTotal()) == 0);
        assertEquals(SupplyStatus.UNPAID, savedSupply.getStatus());
        assertEquals(invoiceId, savedSupply.getInvoiceId());
        assertNotNull(savedSupply.getId());
    }

    @Test
    @DisplayName("Should thrown jpa data error when base field < invoiceId is null")
    public void createSupply_nullableField_throwError(){
        Supply supply = Supply.builder() 
        .id(UuidCreator.getTimeOrderedEpochFast())
        .supplier(setupSupplier)
        .supplierName(setupSupplier.getName())
        .build();
        
        assertNull(supply.getInvoiceId());
        assertThrows(DataIntegrityViolationException.class, () -> supplyRepository.saveAndFlush(supply));
    }

    @Test
    @DisplayName("Should return trx entity when invoiceId is valid")
    public void findByInvoiceId_validId_returnEntity(){
        String invoiceId = invoiceGenerator.generate();

        Supply supply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .supplier(setupSupplier)
        .supplierName(setupSupplier.getName())
        .build();

        supplyRepository.saveAndFlush(supply);

        Supply searchedSupply = supplyRepository.findByInvoiceId(invoiceId).orElseThrow();

        assertEquals(invoiceId, searchedSupply.getInvoiceId());
        assertNotNull(searchedSupply.getCreatedAt());
        assertNotNull(searchedSupply.getId());
    }

    @Test
    @DisplayName("Should return optional of empty when invoiceId is not valid")
    public void findByInvoiceId_invalidId_returnOptionalEmptyEntity(){

        assertThrows(NoSuchElementException.class, () -> supplyRepository.findByInvoiceId("S").orElseThrow());

    }

    @Test
    @DisplayName("Should return filtered supply case 1: status.CANCELLED")
    public void findAllCriteria_statusCancelled_returnCancelledSupply(){
        Set<Supply> pendingSupplys = new HashSet<>();
        
        for (int i = 1; i < 3; i++) {
            if(i%2 == 0) {
                Supply supply = Supply.builder()
                    .id(UuidCreator.getTimeOrderedEpochFast())
                    .invoiceId(invoiceGenerator.generate())
                    .status(SupplyStatus.CANCELLED)
                    .supplier(setupSupplier)
                    .supplierName(setupSupplier.getName())
                    .build();

                 pendingSupplys.add(supply);
            } else {
                Supply supply = Supply.builder()
                    .id(UuidCreator.getTimeOrderedEpochFast())
                    .invoiceId(invoiceGenerator.generate())
                    .supplier(setupSupplier)
                    .supplierName(setupSupplier.getName())
                    .build();

                 pendingSupplys.add(supply);
            }
        }

        supplyRepository.saveAllAndFlush(pendingSupplys);

        SupplyGetByFilter request = SupplyGetByFilter.builder()
        .status(SupplyStatus.CANCELLED)
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<Supply> specification = jpaSpecGenerator.supplySpecification(request);
         
        Slice<Supply> supplies = supplyRepository.findAll(specification, pageable);
        assertEquals(1, supplies.getNumberOfElements());
        assertEquals(SupplyStatus.CANCELLED, supplies.getContent().getFirst().getStatus());

    }

    @Test
    @DisplayName("Should return filtered supply case 2: combine: status.PENDING && totalPaid between 100-200 && createdAt betwenn 2020-2021")
    public void findAllCriteria_combineFilter_returnCombinedFilter(){
        String matchInvoiceId = invoiceGenerator.generate();

        Supply matchSupply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(matchInvoiceId)
        .totalPaid(BigDecimal.valueOf(150L))
        .supplier(setupSupplier)
        .supplierName(setupSupplier.getName())
        .build();

        matchSupply.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        Supply unmatchSupply2 = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceGenerator.generate())
        .totalPaid(BigDecimal.valueOf(350L))
        .supplier(setupSupplier)
        .supplierName(setupSupplier.getName())
        .build();

        matchSupply.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        Supply unmatchSupply3 = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceGenerator.generate())
        .status(SupplyStatus.CANCELLED)
        .totalPaid(BigDecimal.valueOf(150L))
        .supplier(setupSupplier)
        .supplierName(setupSupplier.getName())
        .build();

        matchSupply.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        supplyRepository.saveAllAndFlush(List.of(matchSupply, unmatchSupply2, unmatchSupply3));

        SupplyGetByFilter request = SupplyGetByFilter.builder()
        .status(SupplyStatus.UNPAID)
        .minTotalPaid(BigDecimal.valueOf(100L))
        .maxTotalPaid(BigDecimal.valueOf(200L))
        .minCreatedAt(LocalDateTime.of(2020, 1, 10, 10, 10))
        .maxCreatedAt(LocalDateTime.of(2027, 1, 10, 10, 10))
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<Supply> specification = jpaSpecGenerator.supplySpecification(request);

        Slice<Supply> supplies = supplyRepository.findAll(specification, pageable);
        log.info("haleak, {}" + supplies.getContent().getFirst().getCreatedAt());
        assertEquals(1, supplies.getNumberOfElements());
        assertEquals(SupplyStatus.UNPAID, supplies.getContent().getFirst().getStatus());
        assertEquals(matchInvoiceId, supplies.getContent().getFirst().getInvoiceId());
    }

    @Test
    @DisplayName("Should return filtered supply case 3: get only supplier 1")
    public void findAllCriteria_supplierCase_returnCombinedFilter(){
        String matchInvoiceId = invoiceGenerator.generate();
 
        Supplier supplier2 = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("Supplier 2")
        .email("TEST1@mail.com")
        .contact("0812314567890") 
        .build();

        Supplier savedSupplier2 = supplierRepository.saveAndFlush(supplier2);;

        Supply matchSupply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(matchInvoiceId)
        .totalPaid(BigDecimal.valueOf(150L))
        .supplier(setupSupplier)
        .supplierName(setupSupplier.getName())
        .build();

        matchSupply.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        Supply unmatchSupply2 = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceGenerator.generate())
        .totalPaid(BigDecimal.valueOf(350L))
        .supplier(savedSupplier2)
        .supplierName(savedSupplier2.getName())
        .build();

        supplyRepository.saveAllAndFlush(List.of(matchSupply, unmatchSupply2));

        SupplyGetByFilter request = SupplyGetByFilter.builder()
        .supplierId(setupSupplier.getId())
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<Supply> specification = jpaSpecGenerator.supplySpecification(request);

        Slice<Supply> supplies = supplyRepository.findAll(specification, pageable); 
        assertEquals(1, supplies.getNumberOfElements());
        assertEquals(setupSupplier.getId(), supplies.getContent().getFirst().getSupplier().getId()); 
    }

    @Test
    @DisplayName("Should return supply_items detail entity (with items > product) when supply id is valid")
    public void findByIdDetail_validId_returnDetailEntity(){
        String invoiceId = invoiceGenerator.generate();

        Supplier supplier2 = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("Supplier 2")
        .email("TEST1@mail.com")
        .contact("0812314567890") 
        .build();

        Supplier savedSupplier2 = supplierRepository.saveAndFlush(supplier2);

        Supply supply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .supplier(savedSupplier2)
        .supplierName(savedSupplier2.getName())
        .build();

       Supply savedSupply = supplyRepository.saveAndFlush(supply);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.saveAndFlush(mockCategorizedProduct);

       SupplyItem supplyItem = SupplyItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .supply(savedSupply)
       .product(savedProduct)
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

        supplyItemRepository.saveAndFlush(supplyItem);
        entityManager.clear();
        Supply detailSupply = supplyRepository.findByIdDetail(savedSupply.getId()).orElseThrow(() -> new NotFoundEntityException(""));

        List<SupplyItem> supplyItems = detailSupply.getSupplyItems();
        assertEquals(1, supplyItems.size());
        assertEquals(savedProduct.getId(), supplyItems.getFirst().getProduct().getId()); 
    }


}
