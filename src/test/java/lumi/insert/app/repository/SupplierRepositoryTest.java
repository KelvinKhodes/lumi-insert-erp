package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.response.SupplierNameResponse;
import lumi.insert.app.utils.generator.JpaSpecGenerator; 

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import({JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
@ActiveProfiles("test")
public class SupplierRepositoryTest  extends TestContainerTest {

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    JpaSpecGenerator specGenerator;

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
    @DisplayName("Should return saved entity when repository save success")
    void saveSupplier_validRequest_returnSaved(){
        Supplier supplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .build();

        Supplier saveAndFlush = supplierRepository.saveAndFlush(supplier);
        assertNotNull(saveAndFlush.getId());
        assertEquals("TEST@mail.com", saveAndFlush.getEmail());
        assertTrue(BigDecimal.ZERO.compareTo(saveAndFlush.getTotalPaid()) == 0);
    }

    @Test
    @DisplayName("Should return true when name already exists")
    void existsSupplierName_validRequest_returnTrue(){
        Supplier supplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .build();

        supplierRepository.saveAndFlush(supplier);
        assertTrue(supplierRepository.existsByName(supplier.getName()));  
    }

    @Test
    @DisplayName("Should return searched name when get name found")
    void getSupplierName_validRequest_returnSliceOfName(){
        Supplier supplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .build();

        supplierRepository.saveAndFlush(supplier); 

        Slice<SupplierNameResponse> names = supplierRepository.getByNameContainingIgnoreCaseAndIdAfter("test", new java.util.UUID(0, 0),PageRequest.of(0, 5));;
        assertEquals(1, names.getNumberOfElements()); 
        assertTrue(names.isLast()); 
        assertEquals(supplier.getName(), names.getContent().getFirst().name()); 
    }

    @Test
    @DisplayName("Should return searched name when get name found")
    void getSupplierName_notFoundAny_returnEmptySlice(){
        Supplier supplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .build();

        supplierRepository.saveAndFlush(supplier); 

        Slice<SupplierNameResponse> names = supplierRepository.getByNameContainingIgnoreCaseAndIdAfter("testoster", new java.util.UUID(0, 0),PageRequest.of(0, 5));;
        assertEquals(0, names.getNumberOfElements());  
    }

    @Test
    @DisplayName("Should return searched name when get name found")
    void getSupplierName_lastIdCase_returnEmptySlice(){
        Supplier supplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .build();

        Supplier supplier2 = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER123")
        .email("TEST123@mail.com")
        .contact("081234512367890") 
        .build();

        supplierRepository.saveAllAndFlush(List.of(supplier, supplier2)); 

        Slice<SupplierNameResponse> names = supplierRepository.getByNameContainingIgnoreCaseAndIdAfter("test", supplier.getId(), PageRequest.of(0, 5));
        assertEquals(1, names.getNumberOfElements());  
    }

    @Test
    @DisplayName("Should return filtered supplier when found case 1: supplier is inactive with total unpaid more than 1000 and less than 1500")
    void getSuppliers_inactiveAndTotalUnpaid_shouldReturnDTO(){
        Supplier matchSupplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TEST")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .isActive(false)
        .totalUnpaid(BigDecimal.valueOf(1200L))
        .build();

        Supplier unmatchSupplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TEST3")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .isActive(false)
        .totalUnpaid(BigDecimal.valueOf(1600L))
        .build();

        Supplier unmatchSupplier1 = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TEST2")
        .email("TEST@mail.com")
        .contact("081234567890") 
        .totalUnpaid(BigDecimal.valueOf(1200L))
        .build();

        supplierRepository.saveAllAndFlush(List.of(matchSupplier, unmatchSupplier, unmatchSupplier1));

        SupplierGetByFilter request = SupplierGetByFilter.builder()
        .isActive(false)
        .minTotalUnpaid(BigDecimal.valueOf(1000L))
        .maxTotalUnpaid(BigDecimal.valueOf(1500L))
        .build();

        Pageable pageable = specGenerator.pageable(request);

        Specification<Supplier> supplierSpecification = specGenerator.supplierSpecification(request);

        Slice<Supplier> suppliers = supplierRepository.findAll(supplierSpecification, pageable);
        assertEquals(1, suppliers.getNumberOfElements());
        assertEquals(matchSupplier.getName(), suppliers.getContent().getFirst().getName());
    }

    @Test
    @DisplayName("Should return filtered supplier when found case 2: Empty Content")
    void getSuppliers_emptyResult_shouldReturnEmptyArrDTO(){

        SupplierGetByFilter request = SupplierGetByFilter.builder()
        .isActive(false)
        .minTotalUnpaid(BigDecimal.valueOf(1000L))
        .maxTotalUnpaid(BigDecimal.valueOf(1500L))
        .build();

        Pageable pageable = specGenerator.pageable(request);

        Specification<Supplier> supplierSpecification = specGenerator.supplierSpecification(request);

        Slice<Supplier> suppliers = supplierRepository.findAll(supplierSpecification, pageable);
        assertEquals(0, suppliers.getNumberOfElements());
        assertEquals(List.of(), suppliers.getContent());
    }


}
