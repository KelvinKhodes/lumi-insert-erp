package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.EmployeeRepository;

import lumi.insert.app.TestContainerTest;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@ActiveProfiles("test")
@Import({AuditorAwareImpl.class, SimpleCacheManager.class})
public class EmployeeRepositoryTest  extends TestContainerTest {
    
    @Autowired
    EmployeeRepository employeeRepository;

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
    void saveEmployee_validEntity_shouldReturnSavedEntity(){
        Employee employee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("TESTEMPLOYE")
        .fullname("TESTEMPLOYE")
        .password("TESTEMPLOYE")
        .joinDate(LocalDateTime.now())
        .build();

        Employee savedEmployee = employeeRepository.save(employee);
        assertNotNull(savedEmployee.getCreatedAt());
        assertEquals("TESTEMPLOYE", savedEmployee.getUsername());
    }

    @Test
    @DisplayName("Should return true when employee found")
    void existsByUsername_validEntity_shouldReturnTrue(){
        Employee employee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("TESTEMPLOYE")
        .fullname("TESTEMPLOYE")
        .password("TESTEMPLOYE")
        .joinDate(LocalDateTime.now())
        .build();

        employeeRepository.save(employee);
        assertTrue(employeeRepository.existsByUsername("TESTEMPLOYE"));    
    }

    @Test
    @DisplayName("Should return false when employee not found")
    void existsByUsername_notFound_shouldReturnFalse(){
        assertFalse(employeeRepository.existsByUsername("TESTEMPLOYE"));    
    }

    @Test
    @DisplayName("Should return saved entity when repository save success")
    void findEmployeeByUsername_validEntity_shouldReturnSavedEntity(){
        Employee employee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("TESTEMPLOYE")
        .fullname("TESTEMPLOYE")
        .password("TESTEMPLOYE")
        .joinDate(LocalDateTime.now())
        .build();

        employeeRepository.saveAndFlush(employee);
        Optional<Employee> byUsername = employeeRepository.findByUsername(employee.getUsername());
        assertTrue(byUsername.isPresent());
    }

    @Test
    @DisplayName("Should return empty when entity not found")
    void findEmployeeByUsername_notFound_shouldReturnEmpty(){

        Optional<Employee> byUsername = employeeRepository.findByUsername("wfafa");
        assertTrue(byUsername.isEmpty());
    }
    
}
