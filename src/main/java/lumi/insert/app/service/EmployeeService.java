package lumi.insert.app.service;
 
import java.util.UUID;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;

import lumi.insert.app.dto.request.EmployeeCreateRequest;
import lumi.insert.app.dto.request.EmployeeUpdateRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.EmployeeResponse;

public interface EmployeeService {
    
    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    EmployeeResponse getEmployee(UUID id);

    SliceIndex<EmployeeResponse> getEmployees(PaginationRequest request);

    boolean isExistsEmployeeByUsername(String username);

    EmployeeResponse resetEmployeePassword(UUID id, String password);

    EmployeeResponse updateEmployee(UUID id, EmployeeUpdateRequest request);

    boolean setEmployeeProfile(UUID id, MultipartFile file);
}
