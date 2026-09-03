package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.AuthToken;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.AuthTokenRepository;
import lumi.insert.app.core.repository.EmployeeRepository;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@ActiveProfiles("test")
@Import({AuditorAwareImpl.class, SimpleCacheManager.class})
public class AuthTokenRepositoryTest extends TestContainerTest  {
    
    @Autowired
    AuthTokenRepository authTokenRepository;

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
    void saveAuthToken_validEntity_shouldReturnSavedEntity(){
        Employee employee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("TESTEMPLOYE")
        .fullname("TESTEMPLOYE")
        .password("TESTEMPLOYE")
        .joinDate(LocalDateTime.now())
        .build();

        Employee savedEmployee = employeeRepository.save(employee);
        
        AuthToken authToken = AuthToken.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .refreshToken("someRefreshToken")
        .employee(savedEmployee)
        .expiredAt(LocalDateTime.now().plusDays(1))
        .build();

        AuthToken savedAuthToken = authTokenRepository.saveAndFlush(authToken);
        assertEquals(savedEmployee.getId(), savedAuthToken.getEmployee().getId());
        assertNotNull(savedAuthToken.getCreatedAt());
    }

    @Test
    @DisplayName("Should return saved entity when entity found")
    void findByRefreshToken_foundEntity_shouldReturnSavedEntity(){
        Employee employee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("TESTEMPLOYE")
        .fullname("TESTEMPLOYE")
        .password("TESTEMPLOYE")
        .joinDate(LocalDateTime.now())
        .build();

        Employee savedEmployee = employeeRepository.save(employee);
        
        AuthToken authToken = AuthToken.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .refreshToken("someRefreshToken")
        .employee(savedEmployee)
        .expiredAt(LocalDateTime.now().plusDays(1))
        .build();

        authTokenRepository.saveAndFlush(authToken);
        Optional<AuthToken> byRefreshToken = authTokenRepository.findByRefreshToken("someRefreshToken");

        assertTrue(byRefreshToken.isPresent());
    }

    @Test
    @DisplayName("Should return empty when entity not found")
    void findByRefreshToken_notFound_shouldReturnSavedEntity(){
        Optional<AuthToken> byRefreshToken = authTokenRepository.findByRefreshToken("someRefreshToken");

        assertTrue(byRefreshToken.isEmpty());
    }

    @Test
    @DisplayName("Should delete entity")
    void deleteByRefreshToken_foundEntity_shouldReturnSavedEntity(){
        Employee employee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("TESTEMPLOYE")
        .fullname("TESTEMPLOYE")
        .password("TESTEMPLOYE")
        .joinDate(LocalDateTime.now())
        .build();

        Employee savedEmployee = employeeRepository.save(employee);
        
        AuthToken authToken = AuthToken.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .refreshToken("someRefreshToken")
        .employee(savedEmployee)
        .expiredAt(LocalDateTime.now().plusDays(1))
        .build();

        authTokenRepository.saveAndFlush(authToken);
        authTokenRepository.deleteByRefreshToken("someRefreshToken");

        Optional<AuthToken> byRefreshToken = authTokenRepository.findByRefreshToken("someRefreshToken");
        assertTrue(byRefreshToken.isEmpty());
    }

}
