package lumi.insert.app.service.implement;

import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.activitycore.repository.ActivityLogRepository;
import lumi.insert.app.dto.request.ActivityLogFilterRequest;
import lumi.insert.app.dto.response.ActivityLogResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.mapper.ActivityLogMapper;
import lumi.insert.app.service.ActivityLogService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    private final ActivityLogMapper activityLogMapper;

    private final JpaSpecGenerator jpaSpecGenerator;

    public ActivityLogServiceImpl(ActivityLogRepository activityLogRepository, ActivityLogMapper activityLogMapper, JpaSpecGenerator jpaSpecGenerator) {
        this.activityLogRepository = activityLogRepository;
        this.activityLogMapper = activityLogMapper;
        this.jpaSpecGenerator = jpaSpecGenerator;
    }

    @Override
    public ActivityLogResponse getActivityLog(UUID id) {
        ActivityLog activityLog = activityLogRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Activity Log not found with ID: {}", id);
                return new NotFoundEntityException("Activity Log with id " + id + " is not found");
            });

        return activityLogMapper.createResponseFromEntity(activityLog);
    }

    @Override
    public Slice<ActivityLogResponse> getActivityLogsByFilter(ActivityLogFilterRequest request) {
      Pageable pageable = jpaSpecGenerator.pageable(request);
      Specification<ActivityLog> specification = jpaSpecGenerator.activityLogSpecification(request);

      Slice<ActivityLog> activityLogs = activityLogRepository.findAll(specification, pageable);

      return activityLogs.map(activityLogMapper::createResponseFromEntity);
    }
}
