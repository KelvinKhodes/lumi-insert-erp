package lumi.insert.app.service.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any; 
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice; 

import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.EmployeeResponse;
import lumi.insert.app.exception.NotFoundEntityException;

@Slf4j
public class EmployeeServiceGetTest extends BaseEmployeeServiceTest{
    
    @Test
    @DisplayName("Should return employee entity when id valid and found")
    void getEmployee_foundEntity_returnEmployeeResponseDTO(){
        when(employeeRepositoryMock.findById(setupEmployee.getId())).thenReturn(Optional.of(setupEmployee));

        EmployeeResponse employeeDTO = employeeServiceMock.getEmployee(setupEmployee.getId());
        log.info("{}", employeeDTO);
        assertEquals(setupEmployee.getId(), employeeDTO.id());
        assertEquals(setupEmployee.getUsername(), employeeDTO.username());
        assertEquals(setupEmployee.getFullname(), employeeDTO.fullname());
    }

    @Test
    @DisplayName("Should throw not found when id not found")
    void getEmployee_notfoundEntity_throwNotFound(){
        when(employeeRepositoryMock.findById(setupEmployee.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> employeeServiceMock.getEmployee(setupEmployee.getId()));
    }

    @Test
    @DisplayName("Should return Slice of employee DTO when id valid and found")
    void getEmployees_foundEntity_returnSliceEmployeeResponseDTO(){
        Page<Employee> slice = new PageImpl<Employee>(List.of(setupEmployee));
        when(employeeRepositoryMock.findAll(any(Pageable.class))).thenReturn(slice);

        SliceIndex<EmployeeResponse> employeeDTO = employeeServiceMock.getEmployees(PaginationRequest.builder().build());
        log.info("{}", employeeDTO);
        assertEquals(1, employeeDTO.getNumberOfElements()); 
        assertEquals(setupEmployee.getFullname(), employeeDTO.getContent().getFirst().fullname());
        verify(employeeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw not found when id not found")
    void getEmployees_0data_return0array(){
        when(employeeRepositoryMock.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        SliceIndex<EmployeeResponse> employeeDTO = employeeServiceMock.getEmployees(PaginationRequest.builder().build());
        log.info("wer {}", employeeDTO);
        assertEquals(0, employeeDTO.getNumberOfElements()); 
        assertEquals(List.of(), employeeDTO.getContent());
        verify(employeeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw not found when id not found")
    void isExistsEmployeeByUsername_exists_returnTrue(){
        when(employeeRepositoryMock.existsByUsername("Test")).thenReturn(true);

        assertTrue(employeeServiceMock.isExistsEmployeeByUsername("Test"));
    }
}
