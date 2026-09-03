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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.CustomerRepository;
import lumi.insert.app.dto.request.CustomerGetByFilter;
import lumi.insert.app.dto.response.CustomerNameResponse;
import lumi.insert.app.utils.generator.JpaSpecGenerator; 
import lumi.insert.app.TestContainerTest;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@ActiveProfiles("test")
@Import({JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
public class CustomerRepositoryTest extends TestContainerTest {

    @Autowired
    CustomerRepository customerRepository;

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
    void saveCustomer_validRequest_returnSaved(){
        Customer customer = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .build();

        Customer saveAndFlush = customerRepository.saveAndFlush(customer);
        assertNotNull(saveAndFlush.getId());
        assertEquals("TEST@mail.com", saveAndFlush.getEmail());
        assertTrue(BigDecimal.ZERO.compareTo(saveAndFlush.getTotalPaid()) == 0);
    }

    @Test
    @DisplayName("Should return saved entity when repository save success")
    void existsCustomerName_validRequest_returnTrue(){
        Customer customer = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .build();

         customerRepository.saveAndFlush(customer);
        assertTrue(customerRepository.existsByName(customer.getName()));  
    }

    @Test
    @DisplayName("Should return saved entity when repository save success")
    void getCustomerName_validRequest_returnSliceOfName(){
        Customer customer = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .build();

        customerRepository.saveAndFlush(customer);
        
        Slice<CustomerNameResponse> names = customerRepository.getByNameContainingIgnoreCaseAndIdAfter("test", new UUID(0, 0), PageRequest.of(0, 5));;
        assertEquals(1, names.getNumberOfElements()); 
        assertTrue(names.isLast()); 
        assertEquals(customer.getName(), names.getContent().getFirst().name()); 
    }

    @Test
    @DisplayName("Should return saved entity when repository save success")
    void getCustomerName_notFoundAny_returnEmptySliceOfName(){
        Customer customer = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .build();

        customerRepository.saveAndFlush(customer);
        
        Slice<CustomerNameResponse> names = customerRepository.getByNameContainingIgnoreCaseAndIdAfter("testre", new UUID(0, 0), PageRequest.of(0, 5));;
        assertEquals(0, names.getNumberOfElements());  
    }

    @Test
    @DisplayName("Should return saved entity when repository save success")
    void getCustomerName_lastIdIndex_returnEmptySliceOfName(){
        Customer customer = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .build();

        Customer customer2 = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER2")
        .email("TEST2@mail.com")
        .contact("081224567890")
        .shippingAddress("Jl. xxx")
        .build();

        customerRepository.saveAllAndFlush(List.of(customer, customer2));
        
        Slice<CustomerNameResponse> names = customerRepository.getByNameContainingIgnoreCaseAndIdAfter("test", customer.getId(), PageRequest.of(0, 5));;
        assertEquals(1, names.getNumberOfElements());  
        assertEquals(customer2.getId(), names.getContent().getFirst().id());  
    }

    @Test
    @DisplayName("Should return filtered customer when found case 1: customer is inactive with total unpaid more than 1000 and less than 1500")
    void getCustomers_inactiveAndTotalUnpaid_shouldReturnDTO(){
        Customer matchCustomer = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .isActive(false)
        .totalUnpaid(BigDecimal.valueOf(1200L))
        .build();

        Customer unMatchCustomer = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER3")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .isActive(false)
        .totalUnpaid(BigDecimal.valueOf(1600L))
        .build();

        Customer unMatchCustomer1 = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("TESTCUSTOMER2")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .totalUnpaid(BigDecimal.valueOf(1200L))
        .build();

        customerRepository.saveAllAndFlush(List.of(matchCustomer, unMatchCustomer, unMatchCustomer1));

        CustomerGetByFilter request = CustomerGetByFilter.builder()
        .isActive(false)
        .minTotalUnpaid(BigDecimal.valueOf(1000L))
        .maxTotalUnpaid(BigDecimal.valueOf(1500L))
        .build();

        Pageable pageable = specGenerator.pageable(request);

        Specification<Customer> customerSpecification = specGenerator.customerSpecification(request);

        Slice<Customer> customers = customerRepository.findAll(customerSpecification, pageable);
        assertEquals(1, customers.getNumberOfElements());
        assertEquals(matchCustomer.getName(), customers.getContent().getFirst().getName());
    }

    @Test
    @DisplayName("Should return filtered customer when found case 2: Empty Content")
    void getCustomers_emptyResult_shouldReturnEmptyArrDTO(){

        CustomerGetByFilter request = CustomerGetByFilter.builder()
        .isActive(false)
        .minTotalUnpaid(BigDecimal.valueOf(1000L))
        .maxTotalUnpaid(BigDecimal.valueOf(1500L))
        .build();

        Pageable pageable = specGenerator.pageable(request);

        Specification<Customer> customerSpecification = specGenerator.customerSpecification(request);

        Slice<Customer> customers = customerRepository.findAll(customerSpecification, pageable);
        assertEquals(0, customers.getNumberOfElements());
        assertEquals(List.of(), customers.getContent());
    }


}
