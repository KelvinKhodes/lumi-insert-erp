package lumi.insert.app.controller.product;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.controller.BaseControllerTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "WAREHOUSE")
public abstract class BaseProductControllerTest extends BaseControllerTest{
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
        new byte[11 * 1024 * 1024]
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
