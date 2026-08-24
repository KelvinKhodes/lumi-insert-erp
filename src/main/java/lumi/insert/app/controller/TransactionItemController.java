package lumi.insert.app.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute; 
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; 
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid; 
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.controller.wrapper.WebResponse;
import lumi.insert.app.dto.request.ItemRefundRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.TransactionItemCreateRequest;
import lumi.insert.app.dto.response.TransactionItemDelete;
import lumi.insert.app.dto.response.TransactionItemResponse; 
import lumi.insert.app.service.TransactionItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller to access {@link TransactionItemService}.
 * Endpoints for managing items within sales transactions.
 * @author KelvinKhodes
 * @since 1.0.0
 */
@RestController
@Slf4j
@Tag(name = "Transaction Items", description = "Endpoints for managing items within sales transactions")
public class TransactionItemController {
    
    @Autowired
    TransactionItemService transactionItemService;

/**
 * Adds a new item to an existing transaction
 */
    @Operation(summary = "Create transaction item", description = "Adds a new item to an existing transaction")
    @ApiResponse(responseCode = "201", description = "Transaction item created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @PostMapping(
        path = "/api/transactions/{id}/items",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER')")
    ResponseEntity<WebResponse<TransactionItemResponse>> createTransactionItem(@Parameter(description = "Transaction ID") @PathVariable(name = "id") UUID id, @Valid @RequestBody TransactionItemCreateRequest request){
        log.debug("Transaction item creation request for transaction ID: {}, request: {}", id, request);
        log.info("Adding item to transaction with ID: {}", id);
        
        TransactionItemResponse resultFromService = transactionItemService.createTransactionItem(id, request);
        log.debug("Transaction item created: {}", resultFromService);
 
        WebResponse<TransactionItemResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/" + resultFromService.id())
        .buildAndExpand(resultFromService.id())
        .toUri();

        log.info("Transaction item created successfully with ID: {}", resultFromService.getId());
        return ResponseEntity.created(location).body(wrappedResult);   
    }

/**
 * Retrieve paginated list of all items in a transaction
 */
    @Operation(summary = "Get transaction items", description = "Retrieve paginated list of all items in a transaction")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved transaction items")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @GetMapping(
        path = "/api/transactions/{transactionId}/items",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<Slice<TransactionItemResponse>>> getTransactionItems(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @ModelAttribute PaginationRequest request){
        log.debug("Transaction items list request for transaction ID: {}, pagination: {}", transactionId, request);
        Slice<TransactionItemResponse> resultFromService = transactionItemService.getTransactionItemsByTransactionId(transactionId, request);
        log.debug("Transaction items found: {}", resultFromService);
 
        WebResponse<Slice<TransactionItemResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);   
    }

/**
 * Retrieve detailed information about a specific transaction item
 */
    @Operation(summary = "Get transaction item by ID", description = "Retrieve detailed information about a specific transaction item")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved transaction item")
    @ApiResponse(responseCode = "404", description = "Transaction item not found")
    @GetMapping(
        path = "/api/transactions/{transactionId}/items/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<TransactionItemResponse>> getTransactionItem(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @Parameter(description = "Item ID") @PathVariable(name = "id") UUID id){
        log.debug("Transaction item search by id request: {}", id);
        TransactionItemResponse resultFromService = transactionItemService.getTransactionItem(id);
        log.debug("Transaction item found: {}", resultFromService);
 
        WebResponse<TransactionItemResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);   
    }

/**
 * Retrieve all items in a transaction for a specific product
 */
    @Operation(summary = "Get transaction items by product", description = "Retrieve all items in a transaction for a specific product")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved transaction items")
    @ApiResponse(responseCode = "404", description = "Transaction or product not found")
    @GetMapping(
        path = "/api/transactions/{transactionId}/items/product/{productId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<WebResponse<Slice<TransactionItemResponse>>> getTransactionItemByProductId(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @Parameter(description = "Product ID") @PathVariable(name = "productId") Long productId){
        log.debug("Transaction items by product request for transaction ID: {}, product ID: {}", transactionId, productId);
        Slice<TransactionItemResponse> resultFromService = transactionItemService.getTransactionByTransactionIdAndProductId(transactionId, productId);
        log.debug("Transaction items found: {}", resultFromService);
 
        WebResponse<Slice<TransactionItemResponse>> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);   
    }

/**
 * Removes an item from a transaction (410 Gone response)
 */
    @Operation(summary = "Delete transaction item", description = "Removes an item from a transaction (410 Gone response)")
    @ApiResponse(responseCode = "410", description = "Transaction item deleted successfully")
    @ApiResponse(responseCode = "404", description = "Transaction item not found")
    @DeleteMapping(
        path = "/api/transactions/{transactionId}/items/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER')")
    ResponseEntity<WebResponse<TransactionItemDelete>> DeleteTransactionItem(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @Parameter(description = "Item ID") @PathVariable(name = "id") UUID id){
        log.info("Deleting transaction item with ID: {}", id);
        TransactionItemDelete resultFromService = transactionItemService.deleteTransactionItem(id);
        log.debug("Transaction item deleted: {}", resultFromService);
 
        WebResponse<TransactionItemDelete> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.status(HttpStatusCode.valueOf(410)).body(wrappedResult);   
    }

/**
 * Updates the quantity of a specific item in a transaction
 */
    @Operation(summary = "Update transaction item quantity", description = "Updates the quantity of a specific item in a transaction")
    @ApiResponse(responseCode = "200", description = "Transaction item quantity updated successfully")
    @ApiResponse(responseCode = "404", description = "Transaction item not found")
    @ApiResponse(responseCode = "400", description = "Invalid quantity")
    @PostMapping(
        path = "/api/transactions/{transactionId}/items/{id}/quantity",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER')")
    ResponseEntity<WebResponse<TransactionItemResponse>> UpdateTransactionItemQuantity(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @Parameter(description = "Item ID") @PathVariable(name = "id") UUID id, @Parameter(description = "New quantity") @RequestBody @RequestParam(name = "quantity") BigDecimal quantity){
        log.info("Updating transaction item quantity for item ID: {} to quantity: {}", id, quantity);
        TransactionItemResponse resultFromService = transactionItemService.updateTransactionItemQuantity(id, quantity);
        log.debug("Transaction item updated: {}", resultFromService);
 
        WebResponse<TransactionItemResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);


        return ResponseEntity.ok(wrappedResult);   
    }

/**
 * Records a refund for a specific item in the transaction
 */
    @Operation(summary = "Refund transaction item", description = "Records a refund for a specific item in the transaction")
    @ApiResponse(responseCode = "200", description = "Transaction item refunded successfully")
    @ApiResponse(responseCode = "404", description = "Transaction item not found")
    @ApiResponse(responseCode = "400", description = "Invalid refund request")
    @PostMapping(
        path = "/api/transactions/{transactionId}/items/{id}/refund",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @PreAuthorize("hasAnyRole('CASHIER')")
    ResponseEntity<WebResponse<TransactionItemResponse>> refundTransactionItem(@Parameter(description = "Transaction ID") @PathVariable(name = "transactionId") UUID transactionId, @Parameter(description = "Item ID") @PathVariable(name = "id") UUID id, @Valid @RequestBody ItemRefundRequest request){
        log.info("Refunding transaction item with ID: {}", id);
        TransactionItemResponse resultFromService = transactionItemService.refundTransactionItem(transactionId, request);
        log.debug("Transaction item refunded: {}", resultFromService);
 
        WebResponse<TransactionItemResponse> wrappedResult = WebResponse.getWrapper(resultFromService, null);

        return ResponseEntity.ok(wrappedResult);   
    }
}
