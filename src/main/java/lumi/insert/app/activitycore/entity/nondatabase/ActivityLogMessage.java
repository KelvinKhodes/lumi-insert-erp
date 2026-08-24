package lumi.insert.app.activitycore.entity.nondatabase;

import org.slf4j.MDC;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.service.implement.MessageProducerServiceImpl;
  
/**
 * Wrapper broker message for  {@link MessageProducerServiceImpl#sendActivityLog(ActivityLogMessage)}. 
 * 
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@EqualsAndHashCode(callSuper = false)
@Data 
@NoArgsConstructor 
@AllArgsConstructor
public class ActivityLogMessage extends ActivityLog{

    private ActivityLog activityLog;
    private String requestId;
    /**
     * Wrapped {@link ActivityLog} and added trace ID.
     * @param log
     */
    public ActivityLogMessage(ActivityLog log){
        super(log.getId(), log.getEntityName(), log.getEntityId(), log.getAction(), log.getActionMessage(), log.getIpAddress());
        this.activityLog = log;
        this.requestId = MDC.get("requestId");
    }
}
