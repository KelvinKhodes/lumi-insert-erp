package lumi.insert.app.controller;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.core.entity.MemoView;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.dto.request.MemoCreateRequest;
import lumi.insert.app.dto.request.MemoUpdateRequest;
import lumi.insert.app.dto.response.MemoResponse; 
import lumi.insert.app.service.MemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller to access {@link MemoService}.
 * Endpoints for managing memos and announcements.
 * @author KelvinKhodes
 * @since 1.0.0
 */
@RestController
@Slf4j
@Tag(name = "Memos", description = "Endpoints for managing memos and announcements")
public class MemoController {
    
    @Autowired
    MemoService memoService;

/**
 * Creates a new memo with optional attachments
 */
    @Operation(summary = "Create new memo", description = "Creates a new memo with optional attachments")
    @ApiResponse(responseCode = "201", description = "Memo created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping(
        path = "/api/memos",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<WebResponse<MemoResponse>> createMemoAPI(@Valid @RequestBody MemoCreateRequest request){
        log.debug("Memo creation request: {}", request);
        log.info("Register request for new memo");
        
        MemoResponse resultFromService = memoService.createMemo(request);
        log.debug("Memo created: {}", resultFromService);

        WebResponse<MemoResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/" + resultFromService.id())
        .buildAndExpand(resultFromService.id())
        .toUri();

        log.info("Memo created successfully with ID: {}", resultFromService.getId());
        return ResponseEntity.created(location).body(wrappedResult);
    }

/**
 * Records that an employee has read a specific memo
 */
    @Operation(summary = "Mark memo as read", description = "Records that an employee has read a specific memo")
    @ApiResponse(responseCode = "200", description = "Memo marked as read successfully")
    @ApiResponse(responseCode = "404", description = "Memo not found")
    @PostMapping(
        path = "/api/memos/{id}/read",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<Boolean>> createMemoViewAPI(@AuthenticationPrincipal EmployeeLogin login, @Parameter(description = "Memo ID") @PathVariable(name = "id") Long id){
        log.debug("Memo read mark request for memo ID: {} by employee:", id);
        MemoView resultFromService = memoService.createMemoView(login, id);
        log.debug("Memo view created: {}", resultFromService);
        Boolean isFailed =  resultFromService.getId().isEmpty();
        WebResponse<Boolean> wrappedResult = WebResponse.getWrapper(!isFailed, null);
 
        return ResponseEntity.ok(wrappedResult);
    }

    
/**
 * Updates the content of an existing memo
 */
    @Operation(summary = "Update memo", description = "Updates the content of an existing memo")
    @ApiResponse(responseCode = "200", description = "Memo updated successfully")
    @ApiResponse(responseCode = "404", description = "Memo not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PatchMapping(
        path = "/api/memos/{id}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<MemoResponse>> updateMemoAPI(@Parameter(description = "Memo ID") @PathVariable(name = "id", required = true) Long id, @Valid @RequestBody MemoUpdateRequest request){
        log.info("Update request for memo with ID: {}", id);
        log.debug("Memo ID: {}. Update request: {}", id, request);
        
        MemoResponse resultFromService = memoService.updateMemo(id, request);
        log.debug("Memo updated: {}", resultFromService);

        WebResponse<MemoResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);
        
        log.info("Successfully updated memo with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Archives a memo to hide it from regular view (OWNER only)
 */
    @Operation(summary = "Archive memo", description = "Archives a memo to hide it from regular view (OWNER only)")
    @ApiResponse(responseCode = "200", description = "Memo archived successfully")
    @ApiResponse(responseCode = "404", description = "Memo not found")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @PreAuthorize("hasAnyRole('OWNER')")
    @PostMapping(
        path = "/api/memos/{id}/archive",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<MemoResponse>> archiveMemoAPI(@Parameter(description = "Memo ID") @PathVariable(name = "id") Long id){
        log.info("Archive request for memo with ID: {}", id);
        MemoResponse resultFromService = memoService.archiveMemo(id);
        log.debug("Memo archived: {}", resultFromService);

        WebResponse<MemoResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);
        
        log.info("Successfully archived memo with ID: {}", id);
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Retrieve detailed information about a specific memo
 */
    @Operation(summary = "Get memo by ID", description = "Retrieve detailed information about a specific memo")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved memo")
    @ApiResponse(responseCode = "404", description = "Memo not found")
    @GetMapping(
        path = "/api/memos/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<MemoResponse>> getMemoAPI(@Parameter(description = "Memo ID") @PathVariable(name = "id") Long id){
        log.debug("Memo search by id request: {}", id);
        MemoResponse resultFromService = memoService.getMemo(id);
        log.debug("Memo found: {}", resultFromService);

        WebResponse<MemoResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);
 
        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Retrieve paginated list of memos for the authenticated employee updated after specified time
 */
    @Operation(summary = "Get memos for current employee", description = "Retrieve paginated list of memos for the authenticated employee updated after specified time")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved memos")
    @GetMapping(
        path = "/api/memos",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<Slice<MemoResponse>>> getMemosAPI(@AuthenticationPrincipal EmployeeLogin login, @Parameter(description = "Filter memos updated after this date (ISO format, defaults to 1 month ago)") @RequestParam(name = "updatedAt", required = false) LocalDateTime time){
        log.debug("Memos list request for employee, updated after: {}", time);
        if(time == null) time = LocalDateTime.now().minusMonths(1);
        Slice<MemoResponse> resultFromService = memoService.getMemos(login, time);
        log.debug("Memos found: {}", resultFromService);

        WebResponse<Slice<MemoResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);
 
        return ResponseEntity.ok(wrappedResult);
    }



}
