package lumi.insert.app.controller.customer;

import java.util.List;
import java.util.UUID; 
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.controller.BaseControllerTest;
import lumi.insert.app.dto.response.CustomerDetailResponse;
import lumi.insert.app.dto.response.CustomerNameResponse;
import lumi.insert.app.dto.response.CustomerResponse; 
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "CASHIER")
public abstract class BaseCustomerControllerTest extends BaseControllerTest{
 
    CustomerDetailResponse customerDetailResponse = new CustomerDetailResponse(UUID.randomUUID(), "Test LTE.", "test@gmail.com", "Test - 00xxx", "St. Test 12 A", null, null, null, null, null, null, null);

    Slice<CustomerResponse> sliceCustomerResponse = new SliceImpl<>(List.of(new CustomerResponse(customerDetailResponse.id(), customerDetailResponse.name(), customerDetailResponse.email(), customerDetailResponse.contact())));

    Slice<CustomerNameResponse> sliceNames = new SliceImpl<>(List.of(new CustomerNameResponse(customerDetailResponse.id(), customerDetailResponse.name())));
 
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
