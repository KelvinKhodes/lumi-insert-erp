package lumi.insert.app.aspect;

import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect; 
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.github.f4b6a3.uuid.UuidCreator;

import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.activitycore.entity.nondatabase.ActivityLogMessage;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.dto.response.Identifiable;

import lumi.insert.app.service.MessageProducerService;

/**
 * AOP that handle {@link ActivityLogger} annotation.<br>
 * Handled after a method finished.
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "app.aop.enabled", havingValue = "true", matchIfMissing = true)
public class ActivityLogAspect {

    @Autowired
    MessageProducerService messageProducerService;

    @Autowired
    AuditorAwareImpl auditorAwareImpl;
    
    @AfterReturning(
        pointcut = "@annotation(activityLog)",
        returning = "response"
    )
    void afterMethod(JoinPoint joinPoint, ActivityLogger activityLog, Object response){ 
        log.debug("ActivityLogAspect triggered action={} entityName={} method={}", activityLog.action(), activityLog.entityName(), joinPoint.getSignature());

        ActivityLog result = ActivityLog.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .action(activityLog.action())
            .actionMessage(activityLog.actionMessage())
            .entityName(activityLog.entityName()) 
            .ipAddress(auditorAwareImpl.getAuditorIpAddress().get())
            .build();

            String auditor = auditorAwareImpl.getCurrentAuditor().get();
            
            result.setCreatedBy(auditor);
            result.setUpdatedBy(auditor);

        if (activityLog.action().equals(ActivityAction.EXPORT_DATA)){
            result.setEntityId("EXPORT MULTIPLE");
        }
        // Check the entityIdFromSingleParam. If true, entityId set by method parameter. 
        if (activityLog.entityIdFromSingleParam()){
            Object[] args = joinPoint.getArgs();
            if(args.length > 0 && args[0] instanceof Identifiable){
                Identifiable idGetter = ((Identifiable) args[0]);
                result.setEntityId(idGetter.getId());
                log.debug("Resolved entityId from single parameter={}", result.getEntityId());
            } else {
                log.debug("Entity id not resolved from single parameter for method={}", joinPoint.getSignature());
            }
        // else false, entityId set by method return value < getId
        } else if(response instanceof Identifiable) {
            result.setEntityId(((Identifiable) response).getId());
            log.debug("Resolved entityId from response={}", result.getEntityId());
        }

        // pass to message producer and will handled by consumer 
        messageProducerService.sendActivityLog(new ActivityLogMessage(result));
        log.info("ActivityLog sent action={} entityName={} entityId={}", activityLog.action(), activityLog.entityName(), result.getEntityId());
    }

    @AfterThrowing(
        pointcut = "@annotation(activityLog)",
        throwing = "ex"
    )
    void afterMethodThrowing(JoinPoint joinPoint, ActivityLogger activityLog, Throwable ex) {
        log.debug("ActivityLogAspect caught exception action={} entityName={} method={} message={}", activityLog.action(), activityLog.entityName(), joinPoint.getSignature(), ex.getMessage(), ex);
    }

}
