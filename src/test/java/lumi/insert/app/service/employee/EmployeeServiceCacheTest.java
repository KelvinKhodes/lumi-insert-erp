package lumi.insert.app.service.employee;


import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.core.repository.EmployeeRepository;
import lumi.insert.app.dto.request.CategoryCreateRequest;
import lumi.insert.app.dto.request.EmployeeCreateRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.mapper.*;
import lumi.insert.app.service.EmployeeService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
public class EmployeeServiceCacheTest extends TestContainerTest {

  @Autowired
  CacheManager cacheManager;

  @Autowired
  EmployeeService employeeService;

  @MockitoBean
  EmployeeRepository employeeRepository;

  @MockitoBean
  BCryptPasswordEncoder passwordEncoder;

  @Spy
  EmployeeMapper employeeMapper = new EmployeeMapperImpl();

  @BeforeEach
  void setUp() {
    if (cacheManager.getCache("employees:first-page") != null) {
      cacheManager.getCache("employees:first-page").clear();
    };
  }

  @Test
  @DisplayName("should cache first-page when condition is met")
  void getCustomers_shouldCacheFirstPage() {
    PaginationRequest request = PaginationRequest.builder()
        .sortBy("createdAt")
        .size(12)
        .build();

    when(employeeRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl(List.of()));

    employeeService.getEmployees(request);
    employeeService.getEmployees(request);

    verify(employeeRepository, times(1)).findAll(any(Pageable.class));

    Cache firstPageCache = cacheManager.getCache("employees:first-page");
    assertNotNull(firstPageCache);
    assertNotNull(firstPageCache.get("createdAt_DESC_12"));
  }

  @Test
  @DisplayName("should not cache first-page when condition is not met")
  void getCustomers_shouldNotCacheFirstPage() {
    PaginationRequest request = PaginationRequest.builder()
        .sortBy("createdAt")
        .size(13)
        .build();

    when(employeeRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl(List.of()));

    employeeService.getEmployees(request);
    employeeService.getEmployees(request);

    verify(employeeRepository, times(2)).findAll(any(Pageable.class));

    Cache firstPageCache = cacheManager.getCache("employees:first-page");
    assertNotNull(firstPageCache);
    assertNull(firstPageCache.get("createdAt_DESC_13"));
  }

  @Test
  @DisplayName("Should evict first page cache when create a employee")
  public void createEmployee_evictCache(){
    Cache cachedEmployees = cacheManager.getCache("employees:first-page");

    cachedEmployees.put("random", "random");

    EmployeeCreateRequest request = EmployeeCreateRequest.builder()
        .username("Yeyes")
        .fullname("Yeyes")
        .password("Yeyes123!")
        .joinDate(LocalDateTime.now())
        .build();

    when(passwordEncoder.encode(any())).thenReturn("password");
    when(employeeRepository.existsByUsername(any())).thenReturn(false);
    when(employeeRepository.save(any())).thenReturn(new Employee());

    employeeService.createEmployee(request);
    assertNull(cachedEmployees.get("random"));
  }




}

