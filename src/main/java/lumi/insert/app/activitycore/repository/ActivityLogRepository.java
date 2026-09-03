package lumi.insert.app.activitycore.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import lumi.insert.app.activitycore.entity.ActivityLog;

/**
 * Repository for {@link ActivityLog} entity.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID>, JpaSpecificationExecutor<ActivityLog> {
    
}
