package lumi.insert.app.service.implement;
   
import java.io.IOException; 
import java.util.UUID; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
 
import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.Employee;
import lumi.insert.app.core.entity.EmployeePicture;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.CloudinaryResponse;
import lumi.insert.app.core.repository.AuthTokenRepository;
import lumi.insert.app.core.repository.EmployeePictureRepository;
import lumi.insert.app.core.repository.EmployeeRepository;
import lumi.insert.app.dto.request.EmployeeCreateRequest;
import lumi.insert.app.dto.request.EmployeeUpdateRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.EmployeeResponse;
import lumi.insert.app.exception.DatabaseInternalException;
import lumi.insert.app.exception.DuplicateEntityException; 
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.StorageActionException;
import lumi.insert.app.mapper.EmployeeMapper;
import lumi.insert.app.service.EmployeeService;
import lumi.insert.app.service.StorageService;

/**
 * Implementation of {@link EmployeeService} providing administrative and profile management for staff members.
 * <p>
 * This service handles sensitive operations including password encryption via {@link BCryptPasswordEncoder},
 * session management by invalidating {@link lumi.insert.app.core.entity.AuthToken} upon critical updates, and employee profile
 * image processing with automated storage cleanup on persistence failure.
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class EmployeeServiceImpl implements EmployeeService{

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    AuthTokenRepository authTokenRepository;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Autowired
    EmployeeMapper employeeMapper; 

    @Autowired
    StorageService storageService;

    @Autowired
    EmployeePictureRepository employeePictureRepository;

    /**
     * Registers a new employee.
     *
     * @param request the employee creation details.
     * @return the mapped {@link EmployeeResponse} of the saved entity.
     * @throws DuplicateEntityException if the username is already taken.
     */
    @Override
    @ActivityLogger(
        entityName = "employees",
        action = ActivityAction.EMPLOYEE_REGISTERED,
        actionMessage = "New employee registered"
    )
    @CacheEvict(
        value = "employees:first-page", allEntries = true
    )
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        log.info("Creating employee username={}", request.getUsername());
        if(employeeRepository.existsByUsername(request.getUsername())) {
            log.debug("Create failed, duplicate username={}", request.getUsername());
            throw new DuplicateEntityException("Employee with username " + request.getUsername() + " already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Employee employee = Employee.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .username(request.getUsername())
            .fullname(request.getFullname())
            .password(encodedPassword)
            .joinDate(request.getJoinDate())
            .build();
        
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created employeeId={}, username={}", savedEmployee.getId(), savedEmployee.getUsername());
        return employeeMapper.createDtoResponseFromEmployee(savedEmployee);
    }

    /**
     * Retrieves an employee profile by their unique identifier.
     *
     * @param id the unique UUID of the employee.
     * @return the found {@link EmployeeResponse}.
     * @throws NotFoundEntityException if the employee does not exist.
     */
    @Override
    public EmployeeResponse getEmployee(UUID id) {
        log.info("Retrieving employee id={}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Get failed, employee not found id={}", id);
                return new NotFoundEntityException("Employee with ID " + id + " was not found");
            });

        return employeeMapper.createDtoResponseFromEmployee(employee);
    }

    /**
     * Retrieves a paginated slice of all employees, sorted by creation date descending.
     *
     * @param request the pagination parameters (page, size).
     * @return a {@link Slice} of {@link EmployeeResponse}.
     */
    @Override
    @Cacheable(
        value = "employees:first-page",
        key = "#request.sortBy + '_' + #request.sortDirection + '_' + #request.getSize",
        condition = "#request.getPage == 0 && #request.getSize == 12"
    )
    public Slice<EmployeeResponse> getEmployees(PaginationRequest request) {
        log.info("Listing employees page={}, size={}", request.getPage(), request.getSize());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("createdAt").descending());
        Slice<Employee> employees = employeeRepository.findAll(pageable);

        return employees.map(employeeMapper::createDtoResponseFromEmployee);
    }

    /**
     * Checks if a username is already occupied in the system.
     *
     * @param username the username to check.
     * @return {@code true} if exists, {@code false} otherwise.
     */
    @Override
    public boolean isExistsEmployeeByUsername(String username) {
        return employeeRepository.existsByUsername(username);
    }

    /**
     * Resets an employee's password and invalidates all existing active sessions.
     * <p>Critical: This operation deletes all associated {@link lumi.insert.app.core.entity.AuthToken}s to force re-login.</p>
     *
     * @param id       the employee identifier.
     * @param password the new plain-text password to be encoded.
     * @return the updated {@link EmployeeResponse}.
     * @throws NotFoundEntityException if the employee ID is invalid.
     */
    @Override
    @ActivityLogger(
        entityName = "employees",
        action = ActivityAction.EMPLOYEE_UPDATED,
        actionMessage = "Employee password changed"
    )
    public EmployeeResponse resetEmployeePassword(UUID id, String password) {
        log.info("Resetting password for employee id={}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Password reset failed, employee not found id={}", id);
                return new NotFoundEntityException("Employee with ID " + id + " was not found");
            });

        String encodedPassword = passwordEncoder.encode(password);

        employee.setPassword(encodedPassword);
        
        Employee savedEmployee = employeeRepository.save(employee);

        authTokenRepository.deleteByEmployeeId(employee.getId());
        return employeeMapper.createDtoResponseFromEmployee(savedEmployee);
    }

    /**
     * Updates employee's informations.
     * <p>If the account set to deactive, all active tokens are revoked.</p>
     *
     * @param id      the identifier of the employee.
     * @param request the update data.
     * @return the updated {@link EmployeeResponse}.
     * @throws DuplicateEntityException if the new username is already in use.
     * @throws NotFoundEntityException  if the employee is not found.
     */
    @Override
    @ActivityLogger(
        entityName = "employees",
        action = ActivityAction.EMPLOYEE_UPDATED,
        actionMessage = "Employee updated"
    )
    @CacheEvict(
        value = "employees:first-page", allEntries = true
    )
    public EmployeeResponse updateEmployee(UUID id, EmployeeUpdateRequest request) {
        log.info("Updating employee id={}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Update failed, employee not found id={}", id);
                return new NotFoundEntityException("Employee with ID " + id + " was not found");
            });

        if(request.getUsername() != null && !employee.getUsername().contains(request.getUsername())){
            if(employeeRepository.existsByUsername(request.getUsername())) {
                log.debug("Update failed, duplicate username={}", request.getUsername());
                throw new DuplicateEntityException("Employee with username " + request.getUsername() + " already exists");
            }
        }

        employeeMapper.updateEmployeeFromDto(request, employee);

        if(!employee.isActive()) authTokenRepository.deleteByEmployeeId(employee.getId());

        return employeeMapper.createDtoResponseFromEmployee(employee);
    }

    /**
     * Updates the employee's profile picture and persists the metadata.
     * <p>
     * Logic includes a safety net: if database persistence fails after a successful 
     * cloud upload, the service will attempt to delete the orphaned image from 
     * the storage provider.
     * </p>
     *
     * @param id   the employee identifier.
     * @param file the multipart image file.
     * @return {@code true} if the profile picture was successfully updated.
     * @throws StorageActionException    if cloud upload fails.
     * @throws DatabaseInternalException if database save fails after upload.
     */
    @Override
    public boolean setEmployeeProfile(UUID id, MultipartFile file){ 
        log.info("Setting profile image for employee id={}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Profile update failed, employee not found id={}", id);
                return new NotFoundEntityException("Employee with ID " + id + " was not found");
            });
        
        String fileName = employee.getUsername() + "-profile";

        String publicId = null;

        try { 
            CloudinaryResponse upload = storageService.uploadImageSync(file.getBytes(), fileName ,"employee");

            EmployeePicture employeePicture = EmployeePicture.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .pictureUrl(upload.getSecureUrl())
            .employee(employee)
            .publicId(upload.getPublicId())
            .build();  

            publicId = upload.getPublicId();

            employeePictureRepository.save(employeePicture);
            employee.setPictureUrl(String.valueOf(upload.getSecureUrl()));
            log.info("Employee profile updated for employeeId={}, publicId={}", id, publicId);
            return true; 
        } catch (IOException e) { 
            log.error("Upload failed for employeeId={}, messages={}", id, e.getMessage());
            throw new StorageActionException("Server couldn't complete the request due to internal problem, try again or contact developer");
        } catch (Exception e) { 
            log.error("Save to database failed for employeeId={}, attempting to delete image at storage. Messages={}", id, e.getMessage()); 

            if(publicId != null) {
                if(!(storageService.deleteImage(publicId))) log.error("Failed to delete image with publicId: {}", publicId);
            }

            throw new DatabaseInternalException("Server couldn't complete the request due to internal problem, try again or contact developer");
        } 
  
    }
    
}
