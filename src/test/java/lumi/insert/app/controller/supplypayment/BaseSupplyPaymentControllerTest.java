package lumi.insert.app.controller.supplypayment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.controller.BaseControllerTest;
import lumi.insert.app.dto.response.SupplyPaymentResponse; 
 
@WithMockUser(username = "admin", roles = "FINANCE")
public abstract class BaseSupplyPaymentControllerTest extends BaseControllerTest{
 
    SupplyPaymentResponse supplyPaymentResponse = new SupplyPaymentResponse(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(10000L), "CLIENT", "LUMI", false, null,null ,null);

    SupplyPaymentResponse supplyRefundResponse = new SupplyPaymentResponse(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(10000L), "LUMI", "CLIENT", true, null, null, null);

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
