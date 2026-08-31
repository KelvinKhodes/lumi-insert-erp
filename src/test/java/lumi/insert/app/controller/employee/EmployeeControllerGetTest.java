package lumi.insert.app.controller.employee;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.EmployeeResponse;
import lumi.insert.app.exception.NotFoundEntityException;


public class EmployeeControllerGetTest extends BaseEmployeeControllerTest{
    
    @Test
    @DisplayName("should return employee entity with status OK when entity found")
    public void getEmployeeAPI_validId_shouldReturnEntity() throws Exception{
        when(employeeService.getEmployee(any(UUID.class))).thenReturn(employeeResponse);

        mockMvc.perform(
            get("/api/employees/" + employeeResponse.id()) 
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(employeeResponse.id().toString()))
        .andExpect(jsonPath("$.data.role").value(employeeResponse.role().toString())) 
        .andExpect(jsonPath("$.errors").isEmpty());
        verify(employeeService, times(1)).getEmployee(any());
    }

    @Test 
    @WithMockUser(username = "admin", roles = "FINANCE")
    public void getEmployeeAPI_publicROle_shouldReturnEntity() throws Exception{
        when(employeeService.getEmployee(any(UUID.class))).thenReturn(employeeResponse);

        mockMvc.perform(
            get("/api/employees/" + employeeResponse.id()) 
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(employeeResponse.id().toString()))
        .andExpect(jsonPath("$.data.role").value(employeeResponse.role().toString())) 
        .andExpect(jsonPath("$.errors").isEmpty());
        verify(employeeService, times(1)).getEmployee(any());
    }

    @Test
    @DisplayName("should return error miss match bad req when id variable is not UUID ")
    public void getEmployeeAPI_missMatchId_shouldReturnBadRequest() throws Exception{
        when(employeeService.getEmployee(any(UUID.class))).thenReturn(employeeResponse);

        mockMvc.perform(
            get("/api/employees/" + true) 
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").isNotEmpty( )) 
        .andExpect(jsonPath("$.data").isEmpty());
        verify(employeeService, times(0)).getEmployee(any());
    }

    @Test
    @DisplayName("should return not found when entity is not found")
    public void getEmployeeAPI_notFound_shouldReturnNotFound() throws Exception{
        when(employeeService.getEmployee(employeeResponse.id())).thenThrow(new NotFoundEntityException("Employee with ID " + employeeResponse.id() + " was not found"));

        mockMvc.perform(
            get("/api/employees/" + employeeResponse.id()) 
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isNotFound()) 
        .andExpect(jsonPath("$.data").isEmpty( )) 
        .andExpect(jsonPath("$.errors").value("Employee with ID " + employeeResponse.id() + " was not found"));
        verify(employeeService, times(1)).getEmployee(employeeResponse.id());
    }

    @Test
    @DisplayName("should return Slice employee entity with status OK when entity found")
    public void getEmployeesAPI_valid_shouldReturnSliceEntity() throws Exception{
        Slice<EmployeeResponse> slice = new SliceImpl<>(List.of(employeeResponse));
        when(employeeService.getEmployees(any(PaginationRequest.class))).thenReturn(new SliceIndex<>(slice));

        mockMvc.perform(
            get("/api/employees") 
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].role").value(employeeResponse.role().toString())) 
        .andExpect(jsonPath("$.errors").isEmpty());
        verify(employeeService).getEmployees(argThat(req -> req.getSize().equals(10)));
    }

    @Test
    @DisplayName("should return Slice employee entity with status OK when entity found")
    public void getEmployeesAPI_noData_shouldReturnSliceEntity() throws Exception{
        Slice<EmployeeResponse> slice = new SliceImpl<>(List.of());
        when(employeeService.getEmployees(any(PaginationRequest.class))).thenReturn(new SliceIndex<>(slice));

        mockMvc.perform(
            get("/api/employees?size=5") 
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.size").value(0)) 
        .andExpect(jsonPath("$.errors").isEmpty());
        verify(employeeService).getEmployees(argThat(req -> req.getSize().equals(5)));
    }

     @Test
    @DisplayName("should return true when username exists")
    public void isUsernameExistsAPI_usernameExists_shouldReturnTrue() throws Exception{ 
        when(employeeService.isExistsEmployeeByUsername("tesset%t")).thenReturn(true);

        mockMvc.perform(
            get("/api/employees/exists?username=tesset%t") 
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true)) 
        .andExpect(jsonPath("$.errors").isEmpty());
        verify(employeeService).isExistsEmployeeByUsername(argThat(req -> req.equals("tesset%t")));
    }
}
