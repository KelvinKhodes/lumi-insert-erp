package lumi.insert.app.controller;

import java.net.URI;
import java.util.UUID;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.CustomerCreateRequest;
import lumi.insert.app.dto.request.CustomerGetByFilter;
import lumi.insert.app.dto.request.CustomerGetNameRequest;
import lumi.insert.app.dto.request.CustomerUpdateRequest; 
import lumi.insert.app.dto.response.CustomerDetailResponse;
import lumi.insert.app.dto.response.CustomerNameResponse;
import lumi.insert.app.dto.response.CustomerResponse;
import lumi.insert.app.exception.ForbiddenRequestException;
import lumi.insert.app.service.CustomerService;

/**
 * REST Controller to access {@link CustomerService}.
 * Endpoints for managing customer information and details.
 * @author KelvinKhodes
 * @since 1.0.0
 */
@RestController
@Slf4j
@Tag(name = "Customers", description = "Endpoints for managing customer information and details")
public class CustomerController {
    
    @Autowired
    CustomerService customerService;

    private final int fileUploadSize = 8 * 1024 * 1024;

/**
 * Creates a new customer with the specified details including location
 */
    @Operation(summary = "Create new customer", description = "Creates a new customer with the specified details including location")
    @ApiResponse(responseCode = "201", description = "Customer created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping(
        path = "/api/customers",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER', 'FINANCE')")
    ResponseEntity<WebResponse<CustomerDetailResponse>> createCustomerAPI(@Valid @RequestBody CustomerCreateRequest request){
        log.debug("Customer creation request: {}", request);
        log.info("Register request for new customer: {}", request.getName());
        
        CustomerDetailResponse resultFromService = customerService.createCustomer(request);

        WebResponse<CustomerDetailResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/" + resultFromService.id())
        .buildAndExpand(resultFromService.id())
        .toUri();

        log.info("Customer created successfully with ID: {}", resultFromService.getId());
        return ResponseEntity.created(location).body(wrappedResult);
    }

/**
 * Retrieve detailed information about a specific customer
 */
    @Operation(summary = "Get customer by ID", description = "Retrieve detailed information about a specific customer")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved customer")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping(
        path = "/api/customers/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<CustomerDetailResponse>> getCustomerAPI(@Parameter(description = "Customer ID") @PathVariable(name = "id") UUID id){
        log.debug("Customer search by id request: {}", id);
        CustomerDetailResponse resultFromService = customerService.getCustomer(id);

        WebResponse<CustomerDetailResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        log.debug("Customer search by id result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Retrieve paginated list of customers with optional filtering
 */
    @Operation(summary = "Get all customers", description = "Retrieve paginated list of customers with optional filtering")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved customers")
    @GetMapping(
        path = "/api/customers",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SliceIndex<CustomerResponse>>> getCustomersAPI(@Valid @ModelAttribute CustomerGetByFilter request){
        log.debug("Customers search request: {}", request);
        SliceIndex<CustomerResponse> resultFromService = customerService.getCustomers(request);

        WebResponse<SliceIndex<CustomerResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);
 
        log.debug("Customers search request result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Search for customers by name with pagination support
 */
    @Operation(summary = "Search customer names", description = "Search for customers by name with pagination support")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matching customers")
    @GetMapping(
        path = "/api/customers/searchName",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SliceIndex<CustomerNameResponse>>> searchCustomerNamesAPI(@Valid @ModelAttribute CustomerGetNameRequest request){
        log.debug("Customer search by name request: {}", request);
        SliceIndex<CustomerNameResponse> resultFromService = customerService.searchCustomerNames(request);

        WebResponse<SliceIndex<CustomerNameResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        log.debug("Customer search by name result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);   
    }

/**
 * Updates information for an existing customer
 */
    @Operation(summary = "Update customer", description = "Updates information for an existing customer")
    @ApiResponse(responseCode = "200", description = "Customer updated successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PatchMapping(
        path = "/api/customers/{id}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER', 'FINANCE')")
    ResponseEntity<WebResponse<CustomerDetailResponse>> updateCustomerAPI(@Parameter(description = "Customer ID") @PathVariable(name = "id") UUID id, @Valid @RequestBody CustomerUpdateRequest request){ 
        log.info("Update request for customer with ID: {}", id);
        log.debug("Customer ID: {}. Update request: {}", id, request);
        
        CustomerDetailResponse resultFromService = customerService.updateCustomer(id, request);
        log.debug("Customer ID: {} updated. Changed value: {}", id, resultFromService);

        WebResponse<CustomerDetailResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);
        
        log.info("Successfully updated customer with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Upload an customer's desc pictures
 */
    @Operation(summary = "Upload and set customer description pictures", description = "Upload an customer's desc pictures")
    @ApiResponse(responseCode = "200", description = "Upload set successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "400", description = "Upload file doesnt meet criteria")
    @PostMapping(
        path = "/api/customers/{id}/pictures",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.ALL_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER', 'FINANCE')")
    ResponseEntity<WebResponse<Boolean>> uploadCustomerPictures(@Parameter(description = "Customer ID") @PathVariable(name = "id") UUID id, @Parameter(description = "File(image) to be store") @RequestParam("files") MultipartFile[] files){
        log.info("Picture upload request for customer with ID: {}", id);
        log.debug("Number of files to upload: {}", files.length);
        
        for (MultipartFile file : files) {
            if(file.isEmpty()) {
                log.debug("Upload picture request for customer ID {} failed, caused: File picture cannot be empty", id);
                throw new ForbiddenRequestException("File picture cannot be empty!"); 
            } 
            if(file.getSize() > fileUploadSize) {
                log.debug("Upload picture request for customer ID {} failed, caused: File size must be less than 8Mb", id);
                throw new ForbiddenRequestException("File size must be less than 8Mb");
            } 
            if(!(file.getContentType().contains("image"))) {
                log.debug("Upload picture request for customer ID {} failed, caused: File format type must be image", id);
                throw new ForbiddenRequestException("File format type must be image"); 
            }  
        }
        
        Boolean resultFromService = customerService.addCustomerPicture(id, files);

        WebResponse<Boolean> wrappedResult = WebResponse.getWrapper(resultFromService, null);
        
        log.info("Successfully uploaded pictures for customer with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);
    }
}
