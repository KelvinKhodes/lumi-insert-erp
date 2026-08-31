package lumi.insert.app.service.authtoken;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; 

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock; 
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import lumi.insert.app.core.entity.AuthToken;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.core.repository.AuthTokenRepository;
import lumi.insert.app.core.repository.EmployeeRepository;
import lumi.insert.app.dto.response.AuthTokenResponse;
import lumi.insert.app.dto.response.EmployeeResponse;
import lumi.insert.app.service.implement.AuthTokenServiceImpl;
import lumi.insert.app.mapper.AuthMapperImpl;
import lumi.insert.app.utils.security.JwtUtils;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public abstract class BaseAuthTokenServiceTest {
    
    @InjectMocks
    AuthTokenServiceImpl authTokenServiceMock;

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    AuthTokenRepository authTokenRepository;

    @Mock
    BCryptPasswordEncoder passwordEncoder;

    @Mock
    AuthMapperImpl authMapper;

    @Mock
    JwtUtils jwtUtils;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    Employee setupEmployee;

    AuthToken setupAuthToken;

    AuthTokenResponse authTokenResponse;

    @BeforeEach
    void setup(){
        setupEmployee = Employee.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("testUsername")
        .fullname("testFullname")
        .password("SECRET")
        .joinDate(LocalDateTime.of(2020, 10, 10, 10, 10))
        .lastIp("xxx.xxx.xxx.xxx")
        .build();

        setupAuthToken = AuthToken.builder()
        .employee(setupEmployee)
        .expiredAt(LocalDateTime.now().plus(7, ChronoUnit.DAYS))
        .refreshToken(UuidCreator.getTimeOrderedEpochFast().toString())
        .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authTokenResponse = new AuthTokenResponse("someAccessToken", UuidCreator.getTimeOrderedEpochFast().toString(), new EmployeeResponse(setupEmployee.getId(), setupEmployee.getUsername(), setupEmployee.getFullname(), setupEmployee.getRole(), LocalDateTime.now()), LocalDateTime.now().plusDays(7), null);
    }
}
