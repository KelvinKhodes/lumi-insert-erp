package lumi.insert.app.controller.authtoken;

import java.time.LocalDateTime;

import com.github.f4b6a3.uuid.UuidCreator;

import lumi.insert.app.controller.BaseControllerTest;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.dto.response.AuthTokenResponse;
import lumi.insert.app.dto.response.EmployeeResponse; 

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")  
public abstract class BaseAuthTokenControllerTest extends BaseControllerTest {

    Employee setupEmployee = Employee.builder()
    .id(UuidCreator.getRandomBasedFast())
    .username("testEmployee")
    .build();

    AuthTokenResponse authTokenResponse = new AuthTokenResponse("someAccessToken", "someRefreshToken",new EmployeeResponse(null, null, null, null, null, null) , LocalDateTime.now().plusDays(7), LocalDateTime.now());

}
