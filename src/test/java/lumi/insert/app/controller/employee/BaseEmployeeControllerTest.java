package lumi.insert.app.controller.employee;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.controller.BaseControllerTest;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.dto.response.EmployeeResponse; 
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "OWNER")
public abstract class BaseEmployeeControllerTest extends BaseControllerTest {
      
    EmployeeResponse employeeResponse = new EmployeeResponse(UUID.randomUUID(), "employeeU", "employeeF", EmployeeRole.CASHIER, null, LocalDateTime.now());
 
    MockMultipartFile mockMultipartFile = new MockMultipartFile(
        "files", 
        "test.png", 
        MediaType.IMAGE_PNG_VALUE, 
        "d".getBytes()
    );

    MockMultipartFile mockBigSize = new MockMultipartFile(
        "files", 
        "test.png", 
        MediaType.IMAGE_PNG_VALUE, 
        new byte[9 * 1024 * 1024]
    );

                
    MockMultipartFile mockBroken = new MockMultipartFile(
        "files", 
        "test.png", 
        MediaType.IMAGE_PNG_VALUE, 
        new byte[0]
    );

    MockMultipartFile mockNotImage = new MockMultipartFile(
        "files", 
        "test.pdf", 
        MediaType.APPLICATION_PDF_VALUE, 
        "d".getBytes()
    );

}
