package lumi.insert.app.controller.activitylog;

import lumi.insert.app.dto.request.ActivityLogFilterRequest;
import lumi.insert.app.exception.NotFoundEntityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ActivityLogControllerTest extends BaseActivityLogControllerTest{

    @Test
    @DisplayName("should return Activity Log Response when id is valid")
    public void getActivityLogAPI_validId_shouldReturnEntity() throws Exception{
        when(activityLogService.getActivityLog(any(UUID.class))).thenReturn(activityLogResponse);
        mockMvc.perform(
                get("/api/activitylogs/" + activityLogResponse.id().toString())
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(activityLogResponse.id().toString()))
            .andExpect(jsonPath("$.data.entityName").value(activityLogResponse.entityName()))
            .andExpect(jsonPath("$.data.entityId").value(activityLogResponse.entityId()))
            .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("should return NotFound Response when request id is not valid")
    public void getActivityLogAPI_invalidId_shouldReturnError() throws Exception{
        when(activityLogService.getActivityLog(any(UUID.class))).thenThrow(new NotFoundEntityException("Activity Log with ID 1 was not found"));
        mockMvc.perform(
                get("/api/activitylogs/" + activityLogResponse.id().toString())
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errors").value("Activity Log with ID 1 was not found"))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("should return Slice of Activity Log Response")
    public void getActivityLogsByFilterAPI_shouldReturnEntity() throws Exception{
        ActivityLogFilterRequest request = ActivityLogFilterRequest.builder()
            .entityName(activityLogResponse.entityName())
            .build();

        when(activityLogService.getActivityLogsByFilter(request)).thenReturn(new SliceImpl<>(List.of(activityLogResponse)));

        mockMvc.perform(
                get("/api/activitylogs/filter?entityName=" + activityLogResponse.entityName())
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(activityLogResponse.id().toString()))
            .andExpect(jsonPath("$.data.content[0].entityName").value(activityLogResponse.entityName()))
            .andExpect(jsonPath("$.data.content[0].entityId").value(activityLogResponse.entityId()))
            .andExpect(jsonPath("$.errors").isEmpty());
    }

}
