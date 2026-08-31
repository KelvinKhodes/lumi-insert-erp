package lumi.insert.app.controller;

import java.net.URI;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.CategoryGetRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter; 
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.dto.request.CategoryCreateRequest;
import lumi.insert.app.dto.request.CategoryUpdateRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.CategoryResponse;
import lumi.insert.app.service.CategoryService;

/**
 * REST Controller to access {@link CategoryService}.
 * Endpoints for managing product categories.
 * @author KelvinKhodes
 * @since 1.0.0
 */
@RestController
@Tag(name = "Categories", description = "Endpoints for managing product categories")
@Slf4j
public class CategoryController {
    
    @Autowired
    CategoryService categoryService;

    /**
     * Creates a new product category with the specified name
     * @param request JSON Value of {@link CategoryCreateRequest}
     */ 
    @Operation(summary = "Create new category", description = "Creates a new product category with the specified name")
    @ApiResponse(responseCode = "201", description = "Category created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping(
        path = "/api/categories",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasAnyRole('WAREHOUSE')")
    ResponseEntity<WebResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryCreateRequest request){
        log.debug("Category creation request: {}", request);
        log.info("Register request for new category: {}", request.getName());

        CategoryResponse resultFromService = categoryService.createCategory(request);
        
        WebResponse<CategoryResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);;

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/" + resultFromService.id())
        .buildAndExpand(resultFromService.id())
        .toUri();

        log.info("Category created succesfully with ID: {}", resultFromService.getId()); 
        return ResponseEntity.created(location).body(wrappedResult);   
    }

    /**
     * Retrieve paginated list of all categories with filtering options 
     */ 
    @Operation(summary = "Get all categories", description = "Retrieve paginated list of all categories with filtering options")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved categories")
    @GetMapping(
        path = "/api/categories",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SliceIndex<CategoryResponse>>> getCategories(@Valid @ModelAttribute CategoryGetRequest request){
        log.debug("Categories search request: {}", request);
        SliceIndex<CategoryResponse> resultFromService = categoryService.getCategories(request);

        WebResponse<SliceIndex<CategoryResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        log.debug("Categories search request result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);   
    }

    /**
     * Retrieve detailed information about a specific category
     */ 
    @Operation(summary = "Get category by ID", description = "Retrieve detailed information about a specific category")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved category")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @GetMapping(
        path = "/api/categories/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<CategoryResponse>> getCategoryById(@Parameter(description = "Category ID") @PathVariable(value = "id") Long id){
        log.debug("Category search by id request: {}", id);
        CategoryResponse resultFromService = categoryService.getCategoryById(id);

        WebResponse<CategoryResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);;

        log.debug("Category search by id result: {}", resultFromService);
        return ResponseEntity.ok(wrappedResult);   
    } 

    /**
     * Updates the name of an existing category
     */
    @Operation(summary = "Update category name", description = "Updates the name of an existing category")
    @ApiResponse(responseCode = "200", description = "Category updated successfully")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PutMapping(
        path = "/api/categories/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasAnyRole('WAREHOUSE')")
    ResponseEntity<WebResponse<CategoryResponse>> editCategory(@Parameter(description = "Category ID") @PathVariable(value = "id", required = true) Long id, @Valid @RequestBody CategoryUpdateRequest request){
        request.setId(id);
        log.info("Edit request for category with ID: {}", id);
        log.debug("Category ID: {}. Edit request: {}", id, request);

        CategoryResponse resultFromService = categoryService.updateCategoryName(request);
        log.debug("Category ID: {} updated. Changed value: {}", id, resultFromService);
        
        WebResponse<CategoryResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);;

        log.info("Succesfully edited category with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);   
    }


    /**
     * Activates a category to make it visible for new products
     */
    @Operation(summary = "Activate category", description = "Activates a category to make it visible for new products")
    @ApiResponse(responseCode = "200", description = "Category activated successfully")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PostMapping(
        path = "/api/categories/{id}/activate",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('WAREHOUSE')")
    ResponseEntity<WebResponse<CategoryResponse>> activateProduct(@Parameter(description = "Category ID") @PathVariable(value = "id", required = true) Long id ){ 
        log.info("Activate request for category with ID: {}", id); 
        
        CategoryResponse resultFromService = categoryService.activateCategory(id);

        WebResponse<CategoryResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);;

        log.info("Successfully activated category with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);   
    }


/**
 * Deactivates a category to prevent using it for new products
 */
    @Operation(summary = "Deactivate category", description = "Deactivates a category to prevent using it for new products")
    @ApiResponse(responseCode = "200", description = "Category deactivated successfully")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PostMapping(
        path = "/api/categories/{id}/deactivate",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('WAREHOUSE')")
    ResponseEntity<WebResponse<CategoryResponse>> deactivateProduct(@Parameter(description = "Category ID") @PathVariable(value = "id", required = true) Long id ){ 
        log.info("Deactivate request for category with ID: {}", id); 
        
        CategoryResponse resultFromService = categoryService.deactivateCategory(id);

        WebResponse<CategoryResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);;

        log.info("Successfully deactivated category with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);   
    }
}
