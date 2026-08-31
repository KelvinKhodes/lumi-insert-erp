package lumi.insert.app.service.customer;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.CustomerRepository;
import lumi.insert.app.dto.request.CustomerCreateRequest;
import lumi.insert.app.dto.request.CustomerGetByFilter;
import lumi.insert.app.dto.request.CustomerUpdateRequest;
import lumi.insert.app.dto.response.CustomerDetailResponse;
import lumi.insert.app.mapper.CategoryMapperImpl;
import lumi.insert.app.mapper.CustomerMapper;
import lumi.insert.app.mapper.CustomerMapperImpl;
import lumi.insert.app.service.CustomerService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
public class CustomerServiceCacheTest extends TestContainerTest {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    CustomerService customerService;

    @MockitoBean
    CustomerRepository customerRepository;

    @MockitoBean
    JpaSpecGenerator jpaSpecGenerator;

    @Spy
    CustomerMapper customerMapper = new CustomerMapperImpl();

    @BeforeEach
    void setUp() {
      if (cacheManager.getCache("customers") != null) {
        cacheManager.getCache("customers").clear();
      }
      if (cacheManager.getCache("customers:first-page") != null) {
        cacheManager.getCache("customers:first-page").clear();
      } ;

    }

    @Test
    @DisplayName("should put cache and evict customers:first-page")
    void createCustomer_shouldPutInCustomersAndEvictFirstPage() {
        when(customerRepository.existsByName(any())).thenReturn(false);

        Cache firstPageCache = cacheManager.getCache("customers:first-page");
        firstPageCache.put("some_key", "dummy_data");

        assertNotNull(firstPageCache.get("some_key"));

        CustomerCreateRequest request = CustomerCreateRequest.builder()
            .name("New Customer")
            .email("random@gmail.com")
            .contact("000000")
            .shippingAddress("Tesssss")
            .build();

        Customer build = Customer.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .name(request.getName())
            .email(request.getEmail())
            .contact(request.getContact())
            .shippingAddress(request.getShippingAddress())
            .build();

        when(customerRepository.save(any())).thenReturn(build);

        CustomerDetailResponse result = customerService.createCustomer(request);

        Cache customersCache = cacheManager.getCache("customers");
        assertNotNull(customersCache);
        assertNotNull(customersCache.get(result.getId()));

        assertNull(firstPageCache.get("some_key"));
    }

    @Test
    @DisplayName("should cache customer by id on getCustomer")
        void getCustomer_shouldCacheResult() {
            UUID id = UUID.randomUUID();
            Customer customer = Customer.builder()
                .id(id)
                .name("John Doe")
                .email("john@gmail.com")
                .build();

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

            CustomerDetailResponse result1 = customerService.getCustomer(id);

            CustomerDetailResponse result2 = customerService.getCustomer(id);

            assertNotNull(result1);
            assertNotNull(result2);

            verify(customerRepository, times(1)).findById(id);

            Cache customersCache = cacheManager.getCache("customers");
            assertNotNull(customersCache);
            assertNotNull(customersCache.get(id));
        }

    @Test
    @DisplayName("should cache first-page when condition is met")
    void getCustomers_shouldCacheFirstPage() {
      CustomerGetByFilter request = CustomerGetByFilter.builder()
          .size(12)
          .sortBy("name")
          .sortDirection("ASC")
          .build();

        when(jpaSpecGenerator.pageable(any())).thenReturn(mock(Pageable.class));
        when(jpaSpecGenerator.customerSpecification(any())).thenReturn(mock(Specification.class));
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl(List.of()));

        customerService.getCustomers(request);
        customerService.getCustomers(request);

        verify(customerRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));

        Cache firstPageCache = cacheManager.getCache("customers:first-page");
        assertNotNull(firstPageCache);
        assertNotNull(firstPageCache.get("name_ASC_12"));
    }

    @Test
    @DisplayName("should update cache customers and evict first-page on updateCustomer")
    void updateCustomer_shouldUpdateCacheAndEvictFirstPage() {
      UUID id = UUID.randomUUID();
      CustomerUpdateRequest request = CustomerUpdateRequest.builder()
          .name("Updated Name")
          .build();

      Customer existingCustomer = Customer.builder()
          .id(id)
          .name("Old Name")
          .build();

      Cache firstPageCache = cacheManager.getCache("customers:first-page");
      assertNotNull(firstPageCache);
      firstPageCache.put("page_key", "dummy_page_data");

      when(customerRepository.findById(id)).thenReturn(Optional.of(existingCustomer));

      CustomerDetailResponse result = customerService.updateCustomer(id, request);

      Cache customersCache = cacheManager.getCache("customers");
      assertNotNull(customersCache);
      assertNotNull(customersCache.get(result.getId()));

      assertNull(firstPageCache.get("page_key"));
    }
    @Test
    @DisplayName("should evict cache customer on addCustomerPicture")
    void addCustomerPicture_shouldEvictCustomerCache() {
      UUID id = UUID.randomUUID();

      Cache customersCache = cacheManager.getCache("customers");
      assertNotNull(customersCache);
      customersCache.put(id, "dummy_customer_data");

      assertNotNull(customersCache.get(id));

      when(customerRepository.findById(id)).thenReturn(Optional.of(Customer.builder().id(id).name("John").build()));

      customerService.addCustomerPicture(id, new MultipartFile[0]);

      assertNull(customersCache.get(id));
    }

}
