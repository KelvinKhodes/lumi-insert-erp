package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.entity.Supply;
import lumi.insert.app.core.entity.SupplyPayment;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.core.repository.SupplyPaymentRepository;
import lumi.insert.app.core.repository.SupplyRepository;
import lumi.insert.app.dto.request.SupplyPaymentGetByFilter;
import lumi.insert.app.utils.generator.InvoiceGenerator;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import({InvoiceGenerator.class, JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
@ActiveProfiles("test")
public class SupplyPaymentRepositoryTest  extends TestContainerTest {

    @Autowired
    SupplyRepository supplyRepository;
    
    @Autowired
    SupplyPaymentRepository supplyPaymentRepository; 

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired 
    InvoiceGenerator invoiceGenerator;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

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
        
        supplier = supplierRepository.save(Supplier.builder().id(UuidCreator.getTimeOrderedEpochFast()).name("Test").contact("test").build());
    }
 

    @Test
    @DisplayName("Should add supply_items entity to database when base field < invoiceId required is valid")
    public void createSupplyPayments_baseField_returnSavedEntity(){
        String invoiceId = invoiceGenerator.generate();

        Supply supply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .supplier(supplier)
        .supplierName(supplier.getName())
        .build();

       Supply savedSupply = supplyRepository.save(supply);
 
        SupplyPayment supplyPayment = SupplyPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .supply(savedSupply)
        .totalPayment(BigDecimal.valueOf(1000L))
        .paymentFrom("Incart Global Lte - 2432131")
        .paymentTo("PT. Juke Ner - BCA 14123124")
        .build();

        SupplyPayment savedSupplyPayment= supplyPaymentRepository.saveAndFlush(supplyPayment);

        assertNotNull(savedSupplyPayment.getCreatedAt()); 
        assertEquals(savedSupply.getId(), savedSupplyPayment.getSupply().getId());
        assertEquals("Incart Global Lte - 2432131", savedSupplyPayment.getPaymentFrom());
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
            SupplyPayment supplyPayment = SupplyPayment.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .supply(savedSupply)
                .totalPayment(BigDecimal.valueOf(1000L * i))
                .paymentFrom("Incart Global Lte - 2432131")
                .paymentTo("PT. Juke Ner - BCA 14123124")
                .build();

            supplyPaymentRepository.saveAndFlush(supplyPayment);
        }
        PageRequest pageable = PageRequest.of(0, 2, Sort.by("createdAt").ascending());
        Slice<SupplyPayment> searchedItem = supplyPaymentRepository.findAllBySupplyId(savedSupply.getId(), pageable);

        assertEquals(2, searchedItem.getNumberOfElements());
        assertTrue(searchedItem.hasNext());
        assertTrue(BigDecimal.valueOf(1000L).compareTo(searchedItem.getContent().getLast().getTotalPayment()) == 0);
    }

    @Test
    @DisplayName("Should return Optional Empty entity when supply and product id is not valid")
    public void findAllBySupplyId_invalidId_returnOptionalEmptyEntity(){
        Slice<SupplyPayment> searchedItem = supplyPaymentRepository.findAllBySupplyId(UUID.randomUUID(), PageRequest.of(0, 2, Sort.by("createdAt").ascending()));

        assertTrue(searchedItem.isEmpty());
    }

    @Test
    @DisplayName("Should return filtered supply payment case 1: totalPayment between 1000 - 2000")
    public void findAllCriteria_totalPaymentBetween_returnTotalPayment1000To2000(){
        String invoiceId = invoiceGenerator.generate();

        Supply supply = Supply.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceId)
            .supplier(supplier)
            .supplierName(supplier.getName())
            .build();

        Supply savedSupply = supplyRepository.save(supply);

        SupplyPayment supplyPaymentSuccess = SupplyPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(1500L))
        .supply(savedSupply)
        .build();

        SupplyPayment supplyPaymentFail = SupplyPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(2100L))
        .supply(savedSupply)
        .build();

        supplyPaymentRepository.saveAllAndFlush(List.of(supplyPaymentSuccess, supplyPaymentFail));

        SupplyPaymentGetByFilter request = SupplyPaymentGetByFilter.builder()
        .minTotalPayment(BigDecimal.valueOf(1000L))
        .maxTotalPayment(BigDecimal.valueOf(2000L))
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<SupplyPayment> specification = jpaSpecGenerator.supplyPaymentSpecification(request);

        Slice<SupplyPayment> supplyPayments = supplyPaymentRepository.findAll(specification, pageable);
        assertEquals(1, supplyPayments.getNumberOfElements());
        assertTrue(BigDecimal.valueOf(1500L).compareTo(supplyPayments.getContent().getFirst().getTotalPayment()) == 0);
    }

    @Test
    @DisplayName("Should return filtered supply payment case 1: totalPayment between 1000 - 3000 and only from trx A")
    public void findAllCriteria_createdAtBetween_returnEmpty(){
        String invoiceIdA = invoiceGenerator.generate();

        Supply supplyA = Supply.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceIdA)
            .supplier(supplier)
            .supplierName(supplier.getName())
            .build();

        Supply supplyB = Supply.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceGenerator.generate())
            .supplier(supplier)
            .supplierName(supplier.getName())
            .build();

        List<Supply> savedSupply = supplyRepository.saveAllAndFlush(List.of(supplyA, supplyB));

        SupplyPayment supplyPaymentSuccess = SupplyPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(1500L))
        .supply(savedSupply.getFirst())
        .build();

        SupplyPayment supplyPaymentFail = SupplyPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(2100L))
        .supply(savedSupply.getLast())
        .build();

        supplyPaymentRepository.saveAllAndFlush(List.of(supplyPaymentSuccess, supplyPaymentFail));

        SupplyPaymentGetByFilter request = SupplyPaymentGetByFilter.builder()
        .minTotalPayment(BigDecimal.valueOf(1000L))
        .maxTotalPayment(BigDecimal.valueOf(3000L))
        .supplyId(savedSupply.getLast().getId())
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<SupplyPayment> specification = jpaSpecGenerator.supplyPaymentSpecification(request);

        Slice<SupplyPayment> supplyPayments = supplyPaymentRepository.findAll(specification, pageable);
        assertEquals(1, supplyPayments.getNumberOfElements());
        assertTrue(BigDecimal.valueOf(2100L).compareTo(supplyPayments.getContent().getFirst().getTotalPayment()) == 0);
    }

}
