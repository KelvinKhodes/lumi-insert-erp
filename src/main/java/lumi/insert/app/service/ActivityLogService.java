package lumi.insert.app.service;

import lumi.insert.app.dto.request.ActivityLogFilterRequest;
import lumi.insert.app.dto.response.ActivityLogResponse;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface ActivityLogService {
    ActivityLogResponse getActivityLog(UUID id);

    Slice<ActivityLogResponse> getActivityLogsByFilter(ActivityLogFilterRequest request);
}
