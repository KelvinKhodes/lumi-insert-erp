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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.SupplierCreateRequest;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.request.SupplierGetNameRequest;
import lumi.insert.app.dto.request.SupplierUpdateRequest; 
import lumi.insert.app.dto.response.SupplierDetailResponse;
import lumi.insert.app.dto.response.SupplierNameResponse; 
import lumi.insert.app.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller to access {@link SupplierService}.
 * Endpoints for managing suppliers and vendor information.
 * @author KelvinKhodes
 * @since 1.0.0
 */
@RestController
@Slf4j
@Tag(name = "Suppliers", description = "Endpoints for managing suppliers and vendor information")
public class SupplierController {
    
    @Autowired
    SupplierService supplierService;

/**
 * Creates a new supplier with contact and location details
 */
    @Operation(summary = "Create new supplier", description = "Creates a new supplier with contact and location details")
    @ApiResponse(responseCode = "201", description = "Supplier created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping(
        path = "/api/suppliers",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'FINANCE')")
    ResponseEntity<WebResponse<SupplierDetailResponse>> createSupplierAPI(@Valid @RequestBody SupplierCreateRequest request){
        log.debug("Supplier creation request: {}", request);
        log.info("Register request for new supplier: {}", request.getName());
        
        SupplierDetailResponse resultFromService = supplierService.createSupplier(request);
        log.debug("Supplier created: {}", resultFromService);

        WebResponse<SupplierDetailResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/" + resultFromService.id())
        .buildAndExpand(resultFromService.id())
        .toUri();

        log.info("Supplier created successfully with ID: {}", resultFromService.getId());
        return ResponseEntity.created(location).body(wrappedResult);
    }

/**
 * Retrieve detailed information about a specific supplier
 */
    @Operation(summary = "Get supplier by ID", description = "Retrieve detailed information about a specific supplier")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved supplier")
    @ApiResponse(responseCode = "404", description = "Supplier not found")
    @GetMapping(
        path = "/api/suppliers/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SupplierDetailResponse>> getSupplierAPI(@Parameter(description = "Supplier ID") @PathVariable(name = "id") UUID id){
        log.debug("Supplier search by id request: {}", id);
        SupplierDetailResponse resultFromService = supplierService.getSupplier(id);

        WebResponse<SupplierDetailResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        log.debug("Supplier search by id result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Retrieve paginated list of suppliers with optional filtering
 */
    @Operation(summary = "Get all suppliers", description = "Retrieve paginated list of suppliers with optional filtering")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved suppliers")
    @GetMapping(
        path = "/api/suppliers",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SliceIndex<SupplierDetailResponse>>> getSuppliersAPI(@Valid @ModelAttribute SupplierGetByFilter request){
        log.debug("Suppliers search request: {}", request);
        SliceIndex<SupplierDetailResponse> resultFromService = supplierService.getSuppliers(request);

        WebResponse<SliceIndex<SupplierDetailResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);
 
        log.debug("Suppliers search request result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Search for suppliers by name with pagination support
 */
    @Operation(summary = "Search supplier names", description = "Search for suppliers by name with pagination support")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matching suppliers")
    @GetMapping(
        path = "/api/suppliers/searchName",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SliceIndex<SupplierNameResponse>>> searchSupplierNamesAPI(@Valid @ModelAttribute SupplierGetNameRequest request){
        log.debug("Supplier search by name request: {}", request);
        SliceIndex<SupplierNameResponse> resultFromService = supplierService.searchSupplierNames(request);

        WebResponse<SliceIndex<SupplierNameResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        log.debug("Supplier search by name result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);   
    }

/**
 * Updates information for an existing supplier
 */
    @Operation(summary = "Update supplier", description = "Updates information for an existing supplier")
    @ApiResponse(responseCode = "200", description = "Supplier updated successfully")
    @ApiResponse(responseCode = "404", description = "Supplier not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PatchMapping(
        path = "/api/suppliers/{id}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'FINANCE')")
    ResponseEntity<WebResponse<SupplierDetailResponse>> updateSupplierAPI(@Parameter(description = "Supplier ID") @PathVariable(name = "id") UUID id, @Valid @RequestBody SupplierUpdateRequest request){ 
        log.info("Update request for supplier with ID: {}", id);
        log.debug("Supplier ID: {}. Update request: {}", id, request);
        
        SupplierDetailResponse resultFromService = supplierService.updateSupplier(id, request);
        log.debug("Supplier ID: {} updated. New value: {}", id, request);

        WebResponse<SupplierDetailResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);
        
        log.info("Successfully updated supplier with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);
    }
}
