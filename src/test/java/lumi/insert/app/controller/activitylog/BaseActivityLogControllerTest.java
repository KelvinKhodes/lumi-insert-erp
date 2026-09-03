package lumi.insert.app.controller.activitylog;

import com.github.f4b6a3.uuid.UuidCreator;
import lumi.insert.app.controller.ActivityLogController;
import lumi.insert.app.controller.BaseControllerTest;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.dto.response.ActivityLogResponse;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@WithMockUser(username = "OWNER", roles = "OWNER")
public class BaseActivityLogControllerTest extends BaseControllerTest {
    public ActivityLogResponse activityLogResponse = new ActivityLogResponse(UuidCreator.getTimeOrderedEpochFast(), "products", "1L", ActivityAction.PRODUCT_CREATED, null, null, null, null);
}
