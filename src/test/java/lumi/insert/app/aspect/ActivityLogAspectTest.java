package lumi.insert.app.aspect;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.dto.response.ProductResponse;
import lumi.insert.app.dto.response.TransactionItemStatisticResponse;
import lumi.insert.app.service.MessageProducerService;

@ExtendWith(MockitoExtension.class) 
@ActiveProfiles("test")
public class ActivityLogAspectTest {
    
    @InjectMocks
    ActivityLogAspect activityLogAspect;

    @Mock
    MessageProducerService messageProducerService;

    @Mock
    AuditorAwareImpl auditorAwareImpl;

    @Mock
    ActivityLogger activityLogger;

    @Mock
    JoinPoint joinPoint;

    @Test
    void afterMethod_shouldSendActivityLog(){
        when(activityLogger.action()).thenReturn(ActivityAction.PRODUCT_CREATED);
        when(activityLogger.actionMessage()).thenReturn("a message");
        when(activityLogger.entityName()).thenReturn("products");
        when(auditorAwareImpl.getAuditorIpAddress()).thenReturn(Optional.of("0.1.2.3"));
        when(auditorAwareImpl.getCurrentAuditor()).thenReturn(Optional.of("0.1.2.3")); 

        ProductResponse productResponse = new ProductResponse(1L, null, null, null, null, null, null, null, null, null, null);
        activityLogAspect.afterMethod(joinPoint, activityLogger, productResponse);

        verify(messageProducerService, times(1)).sendActivityLog(argThat(arg -> arg.getEntityId().equals(String.valueOf(1L)) && arg.getAction() == ActivityAction.PRODUCT_CREATED));
    }

    @Test
    void afterMethod_entityIdGetter_shouldParseEntityIdFromParam(){
        when(activityLogger.action()).thenReturn(ActivityAction.PRODUCT_CREATED);
        when(activityLogger.actionMessage()).thenReturn("a message");
        when(activityLogger.entityName()).thenReturn("products");
        when(activityLogger.entityIdFromSingleParam()).thenReturn(true);
        when(auditorAwareImpl.getAuditorIpAddress()).thenReturn(Optional.of("0.1.2.3")); 
        when(auditorAwareImpl.getCurrentAuditor()).thenReturn(Optional.of("0.1.2.3")); 

        TransactionItemStatisticResponse mockResponse = TransactionItemStatisticResponse.builder()
        .build();
 
        when(joinPoint.getArgs()).thenReturn(new Object[]{mockResponse});
        
        activityLogAspect.afterMethod(joinPoint, activityLogger, null);

        verify(messageProducerService, times(1)).sendActivityLog(argThat(arg -> arg.getEntityId().equals("EXPORT")));
    }

}
