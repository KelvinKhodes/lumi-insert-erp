package lumi.insert.app.controller;

import java.net.URI;
import java.util.UUID;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.TransactionPaymentCreateRequest;
import lumi.insert.app.dto.request.TransactionPaymentGetByFilter;
import lumi.insert.app.dto.response.TransactionPaymentResponse;
import lumi.insert.app.exception.ForbiddenRequestException;
import lumi.insert.app.service.TransactionPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller to access {@link TransactionPaymentService}.
 * Endpoints for managing transaction payments and collections.
 * @author KelvinKhodes
 * @since 1.0.0
 */
@RestController
@Transactional
@Slf4j
@Tag(name = "Transaction Payments", description = "Endpoints for managing transaction payments and collections")
public class TransactionPaymentController {
    
    @Autowired
    TransactionPaymentService transactionPaymentService;

    private final int fileUploadSize = 8 * 1024 * 1024;

/**
 * Records a new payment received for a transaction
 */
    @Operation(summary = "Create transaction payment", description = "Records a new payment received for a transaction")
    @ApiResponse(responseCode = "201", description = "Transaction payment created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @PostMapping(
        path = "/api/transactions/{transactionId}/payments",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER', 'FINANCE')")
    ResponseEntity<WebResponse<TransactionPaymentResponse>> createTransactionPaymentAPI(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @ModelAttribute @Valid @RequestBody TransactionPaymentCreateRequest request){
        log.info("Create transaction payment request for transaction ID: {}", transactionId);
        log.debug("Payment creation request: {}", request);
        
        for (MultipartFile file : request.getFiles()) {
            if(file.isEmpty()) throw new ForbiddenRequestException("File picture cannot be empty!");
            if(file.getSize() > fileUploadSize) throw new ForbiddenRequestException("File size must be less than 8Mb");
            if(!(file.getContentType().contains("image"))) throw new ForbiddenRequestException("File format type must be image");  
        }

        TransactionPaymentResponse resultFromService = transactionPaymentService.createTransactionPayment(transactionId, request);
        log.debug("Transaction payment created: {}", resultFromService);
        WebResponse<TransactionPaymentResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/" + resultFromService.id())
        .buildAndExpand(resultFromService.id())
        .toUri();

        log.info("Transaction payment created successfully with ID: {}", resultFromService.getId());
        return ResponseEntity.created(location).body(wrappedResult);
    }

/**
 * Records a refund for a payment in a transaction
 */
    @Operation(summary = "Refund transaction payment", description = "Records a refund for a payment in a transaction")
    @ApiResponse(responseCode = "201", description = "Transaction payment refund created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @PostMapping(
        path = "/api/transactions/{transactionId}/payments/refund",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER', 'FINANCE')")
    ResponseEntity<WebResponse<TransactionPaymentResponse>> refundTransactionPaymentAPI(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @ModelAttribute @Valid @RequestBody TransactionPaymentCreateRequest request){
        log.info("Refund transaction payment request for transaction ID: {}", transactionId);
        log.debug("Payment refund request: {}", request);
        
        for (MultipartFile file : request.getFiles()) {
            if(file.isEmpty()) throw new ForbiddenRequestException("File picture cannot be empty!");
            if(file.getSize() > fileUploadSize) throw new ForbiddenRequestException("File size must be less than 8Mb");
            if(!(file.getContentType().contains("image"))) throw new ForbiddenRequestException("File format type must be image");  
        }
        
        TransactionPaymentResponse resultFromService = transactionPaymentService.refundTransactionPayment(transactionId, request);
        log.debug("Transaction payment refunded: {}", resultFromService);
        WebResponse<TransactionPaymentResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/" + resultFromService.id())
        .buildAndExpand(resultFromService.id())
        .toUri();

        log.info("Transaction payment refund created successfully with ID: {}", resultFromService.getId());
        return ResponseEntity.created(location).body(wrappedResult);
    }

/**
 * Retrieve detailed information about a specific transaction payment
 */
    @Operation(summary = "Get transaction payment by ID", description = "Retrieve detailed information about a specific transaction payment")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved transaction payment")
    @ApiResponse(responseCode = "404", description = "Transaction payment not found")
    @GetMapping(
        path = "/api/transactions/{transactionId}/payments/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<TransactionPaymentResponse>> getTransactionPaymentAPI(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @Parameter(description = "Payment ID") @PathVariable(name = "id") UUID id){
        log.debug("Transaction payment search by id request: {}", id);
        TransactionPaymentResponse resultFromService = transactionPaymentService.getTransactionPayment(id);
        log.debug("Transaction payment found: {}", resultFromService);

        WebResponse<TransactionPaymentResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Retrieve paginated list of all payments for a transaction
 */
    @Operation(summary = "Get transaction payments", description = "Retrieve paginated list of all payments for a transaction")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved transaction payments")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @GetMapping(
        path = "/api/transactions/{transactionId}/payments",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<Slice<TransactionPaymentResponse>>> getTransactionPaymentsAPI(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId,@ModelAttribute @Valid PaginationRequest request){
        log.debug("Transaction payments list request for transaction ID: {}, pagination: {}", transactionId, request);
        Slice<TransactionPaymentResponse> resultFromService = transactionPaymentService.getTransactionPaymentsByTransactionId(transactionId, request);
        log.debug("Transaction payments found: {}", resultFromService);

        WebResponse<Slice<TransactionPaymentResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);
    }

/**
 * Search transaction payments with filtering options
 */
    @Operation(summary = "Search transaction payments", description = "Search transaction payments with filtering options")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered transaction payments")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @GetMapping(
        path = "/api/transactions/{transactionId}/payments/filter",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SliceIndex<TransactionPaymentResponse>>> searchTransactionPaymentsFilter(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @ModelAttribute @Valid TransactionPaymentGetByFilter request){
        log.debug("Transaction payments search request with filter: {}", request);
        SliceIndex<TransactionPaymentResponse> resultFromService = transactionPaymentService.getTransactionPaymentsByRequests(request);
        log.debug("Transaction payments found: {}", resultFromService);

        WebResponse<SliceIndex<TransactionPaymentResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);
    }

    /**
     * Search global transactions payments with filtering options
     */
    @Operation(summary = "Search global transactions payments", description = "Search global ransactions payments with filtering options")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered transaction payments")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @GetMapping(
        path = "/api/transactions/payments/filter",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<SliceIndex<TransactionPaymentResponse>>> searchGlobalTransactionsPaymentsFilter(@ModelAttribute @Valid TransactionPaymentGetByFilter request){
        log.debug("Transaction payments search request with filter: {}", request);
        SliceIndex<TransactionPaymentResponse> resultFromService = transactionPaymentService.getTransactionPaymentsByRequests(request);
        log.debug("Transaction payments found: {}", resultFromService);

        WebResponse<SliceIndex<TransactionPaymentResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);
    }
}
