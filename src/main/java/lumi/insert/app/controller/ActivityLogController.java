package lumi.insert.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.dto.request.ActivityLogFilterRequest;
import lumi.insert.app.dto.request.ProductGetByFilter;
import lumi.insert.app.dto.response.ActivityLogResponse;
import lumi.insert.app.service.ActivityLogService;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller to access activity logs services.
 * Endpoints for fetching data of activity logs.
 * @author KelvinKhodes
 * @since 1.0.0
 */
@RestController
@Tag(name = "Activity Logs", description = "Endpoints for view-only fetch activity logs")
@Slf4j
public class ActivityLogController {

  private final ActivityLogService activityLogService;

  public ActivityLogController(ActivityLogService activityLogService) {
    this.activityLogService = activityLogService;
  }

  /**
   * Retrieve detail of Activity Log
   */
  @Operation(summary = "Get activity log", description = "Retrieve detail of Activity Log")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved activity log")
  @GetMapping(
      path = "/api/activitylogs/{id}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  ResponseEntity<WebResponse<ActivityLogResponse>> getActivityLog(@Parameter(description = "Activity Log ID") @PathVariable(name = "id") UUID id){
    ActivityLogResponse resultFromService = activityLogService.getActivityLog(id);

    WebResponse<ActivityLogResponse> result = WebResponse.getWrapper(resultFromService, null);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Get list of activity logs", description = "Retrieve paginated Activity Log")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved")
  @GetMapping(
      path = "/api/activitylogs/filter",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  ResponseEntity<WebResponse<Slice<ActivityLogResponse>>> getActivityLogsByFilter(@ModelAttribute @Valid ActivityLogFilterRequest request){
    Slice<ActivityLogResponse> resultFromService = activityLogService.getActivityLogsByFilter(request);

    WebResponse<Slice<ActivityLogResponse>> result = WebResponse.getWrapper(resultFromService, null);
    return ResponseEntity.ok(result);
  }
}
