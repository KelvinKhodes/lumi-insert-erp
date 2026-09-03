package lumi.insert.app.service.activitylog;

import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.dto.request.ActivityLogFilterRequest;
import lumi.insert.app.dto.response.ActivityLogResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActivityLogServiceTest extends BaseActivityLogServiceTest{

    @Test
    @DisplayName("should return Activity Log Response")
    void getActivityLog_entityValid_returnResponse(){
        when(activityLogRepository.findById(activityLog.getId())).thenReturn(Optional.of(activityLog));

        ActivityLogResponse result = activityLogService.getActivityLog(activityLog.getId());
        assertEquals(activityLog.getEntityName(), result.entityName());
        verify(activityLogRepository, times(1)).findById(any());
    }

    @Test
    @DisplayName("should throw not found exception ")
    void getActivityLog_notFound_throwExc(){
        when(activityLogRepository.findById(activityLog.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> activityLogService.getActivityLog(activityLog.getId()));
    }

    @Test
    @DisplayName("should return Slice of Activity Log Response")
    void getActivityLogsByFilter_validRequest(){
        ActivityLogFilterRequest request = ActivityLogFilterRequest.builder()
            .size(12)
            .build();

        Pageable pageRequest = PageRequest.ofSize(request.getSize());
        Specification<ActivityLog> specification = Specification.anyOf(List.of());

        when(jpaSpecGenerator.pageable(request)).thenReturn(pageRequest);
        when(jpaSpecGenerator.activityLogSpecification(request)).thenReturn(specification);
        when(activityLogRepository.findAll(specification, pageRequest)).thenReturn(new PageImpl<>(List.of(activityLog)));

        Slice<ActivityLogResponse> activityLogsByFilter = activityLogService.getActivityLogsByFilter(request);
        verify(jpaSpecGenerator, times(1)).pageable(request);
        verify(jpaSpecGenerator, times(1)).activityLogSpecification(request);
        verify(activityLogRepository, times(1)).findAll(specification, pageRequest);

        assertEquals(1, activityLogsByFilter.getNumberOfElements());
        assertEquals(activityLog.getId(), activityLogsByFilter.getContent().getFirst().id());

    }
}
