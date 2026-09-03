package lumi.insert.app.service.activitylog;

import com.github.f4b6a3.uuid.UuidCreator;
import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.activitycore.repository.ActivityLogRepository;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.mapper.ActivityLogMapper;
import lumi.insert.app.mapper.ActivityLogMapperImpl;
import lumi.insert.app.service.ActivityLogService;
import lumi.insert.app.service.implement.ActivityLogServiceImpl;
import lumi.insert.app.utils.generator.JpaSpecGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public abstract  class BaseActivityLogServiceTest {
    @InjectMocks
    ActivityLogServiceImpl activityLogService;

    @Mock
    ActivityLogRepository activityLogRepository;

    @Mock
    JpaSpecGenerator jpaSpecGenerator;

    @Spy
    ActivityLogMapper mapper = new ActivityLogMapperImpl();

    ActivityLog activityLog;
    @BeforeEach
    void setup (){
        activityLog = ActivityLog.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .entityId("1L")
            .entityName("products")
            .action(ActivityAction.PRODUCT_CREATED)
            .ipAddress("0.0.0.0")
            .build();
    }
}
