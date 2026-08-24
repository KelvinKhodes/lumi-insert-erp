package lumi.insert.app.controller.transactionitem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when; 
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import lumi.insert.app.dto.request.ItemRefundRequest;
import lumi.insert.app.exception.ForbiddenRequestException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.TransactionValidationException;

public class TransactionItemControllerUpdateTest extends BaseTransactionItemControllerTest{
    
    @Test
    @DisplayName("should return transaction item dto when request Trx item to update quantity success")
    public void updateTransactionItemQuantityAPI_validId_shouldReturnDTO() throws Exception{  
        when(transactionItemService.updateTransactionItemQuantity(transactionItemResponse.id(), BigDecimal.valueOf(5L))).thenReturn(transactionItemResponse);

        mockMvc.perform(
            post("/api/transactions/" + UUID.randomUUID().toString() + "/items/" + transactionItemResponse.id().toString() + "/quantity")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
        )
        .andDo(print()) 
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(transactionItemResponse.id().toString()))
        .andExpect(jsonPath("$.data.quantity").value(5L))
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "OWNER")
    public void updateTransactionItemQuantityAPI_higherRole_shouldReturnDTO() throws Exception{  
        when(transactionItemService.updateTransactionItemQuantity(transactionItemResponse.id(), BigDecimal.valueOf(5L))).thenReturn(transactionItemResponse);

        mockMvc.perform(
            post("/api/transactions/" + UUID.randomUUID().toString() + "/items/" + transactionItemResponse.id().toString() + "/quantity")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
        )
        .andDo(print()) 
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(transactionItemResponse.id().toString()))
        .andExpect(jsonPath("$.data.quantity").value(5L))
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "WAREHOUSE")
    public void updateTransactionItemQuantityAPI_invalidRole_shouldReturnDTO() throws Exception{  
        when(transactionItemService.updateTransactionItemQuantity(transactionItemResponse.id(), BigDecimal.valueOf(5L))).thenReturn(transactionItemResponse);

        mockMvc.perform(
            post("/api/transactions/" + UUID.randomUUID().toString() + "/items/" + transactionItemResponse.id().toString() + "/quantity")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
        )
        .andDo(print()) 
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Access denied, require an authority")); 
    }

    @Test
    @DisplayName("should return error not found when request Trx item Not Found")
    public void updateTransactionItemQuantityAPI_invalidId_shouldReturnErrorNotFound() throws Exception{ 
        when(transactionItemService.updateTransactionItemQuantity(transactionItemResponse.id(), BigDecimal.valueOf(5L))).thenThrow(new NotFoundEntityException("Transaction Items with ID " + 1L + " was not found"));
        
       mockMvc.perform(
            post("/api/transactions/" + UUID.randomUUID().toString() + "/items/" + transactionItemResponse.id().toString() + "/quantity")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
        )
        .andDo(print()) 
        .andExpect(status().isNotFound()) 
        .andExpect(jsonPath("$.errors").value("Transaction Items with ID " + 1L + " was not found"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("should return error forbiddenrequest when request Transaction is not pending")
    public void updateTransactionItemQuantityAPI_nonPendingTrx_shouldReturnErrorForbiddenRequest() throws Exception{ 
        when(transactionItemService.updateTransactionItemQuantity(transactionItemResponse.id(), BigDecimal.valueOf(5L))).thenThrow(new ForbiddenRequestException("Couldn't delete the item because Transaction Status is not PENDING(CART)"));

       mockMvc.perform(
            post("/api/transactions/" + UUID.randomUUID().toString() + "/items/" + transactionItemResponse.id().toString() + "/quantity")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
        )
        .andDo(print()) 
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").value("Couldn't delete the item because Transaction Status is not PENDING(CART)"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("should return error transactionvalidation when request product stock lesser than requested quantity")
    public void updateTransactionItemQuantityAPI_outOfStock_shouldReturnErrorTransactionValidation() throws Exception{ 
        when(transactionItemService.updateTransactionItemQuantity(transactionItemResponse.id(), BigDecimal.valueOf(10L))).thenThrow(new TransactionValidationException("Product stocks with ID " + 1L + " doesn't meet buyer quantity, stock left: " + 5L));

       mockMvc.perform(
            post("/api/transactions/" + UUID.randomUUID().toString() + "/items/" + transactionItemResponse.id().toString() + "/quantity")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "10")
        )
        .andDo(print()) 
        .andExpect(status().isUnprocessableContent()) 
        .andExpect(jsonPath("$.errors").value("Product stocks with ID " + 1L + " doesn't meet buyer quantity, stock left: " + 5L))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("should return transaction item dto when refund Trx item success")
    public void refundTransactionItemAPI_validId_shouldReturnDTO() throws Exception{
        UUID transactionId = UUID.randomUUID();
        when(transactionItemService.refundTransactionItem(eq(transactionId), any(ItemRefundRequest.class))).thenReturn(transactionItemResponse);

        mockMvc.perform(
            post("/api/transactions/" + transactionId.toString() + "/items/" + transactionItemResponse.id().toString() + "/refund")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
            .param("productId", "1")
        )
        .andDo(print()) 
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(transactionItemResponse.id().toString()))
        .andExpect(jsonPath("$.data.quantity").value(5L))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(transactionItemService, times(1)).refundTransactionItem(any(), argThat(arg -> arg.getProductId() == 1L));
    }

    @Test 
    @WithMockUser(username = "admin", roles = "OWNER")
    public void refundTransactionItemAPI_higherRole_shouldReturnDTO() throws Exception{
        UUID transactionId = UUID.randomUUID();
        when(transactionItemService.refundTransactionItem(eq(transactionId), any(ItemRefundRequest.class))).thenReturn(transactionItemResponse);

        mockMvc.perform(
            post("/api/transactions/" + transactionId.toString() + "/items/" + transactionItemResponse.id().toString() + "/refund")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
            .param("productId", "1")
        )
        .andDo(print()) 
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(transactionItemResponse.id().toString()))
        .andExpect(jsonPath("$.data.quantity").value(5L))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(transactionItemService, times(1)).refundTransactionItem(any(), argThat(arg -> arg.getProductId() == 1L));
    }

    @Test 
    @WithMockUser(username = "admin", roles = "WAREHOUSE")
    public void refundTransactionItemAPI_invalidRole_shouldReturnDTO() throws Exception{  
        when(transactionItemService.refundTransactionItem(eq(transactionItemResponse.id()), any(ItemRefundRequest.class))).thenReturn(transactionItemResponse);

        mockMvc.perform(
            post("/api/transactions/" + UUID.randomUUID().toString() + "/items/" + transactionItemResponse.id().toString() + "/refund")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
            .param("productId", "1")
        )
        .andDo(print()) 
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Access denied, require an authority")); 
    }

    @Test
    @DisplayName("should return error not found when request Trx item Not Found")
    public void refundTransactionItemAPI_invalidId_shouldReturnErrorNotFound() throws Exception{
        UUID transactionId = UUID.randomUUID();
        when(transactionItemService.refundTransactionItem(eq(transactionId), any(ItemRefundRequest.class))).thenThrow(new NotFoundEntityException("Transaction Items with ID " + 1L + " was not found"));
        
        mockMvc.perform(
            post("/api/transactions/" + transactionId.toString() + "/items/" + transactionItemResponse.id().toString() + "/refund")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
            .param("productId", "1")
        )
        .andDo(print()) 
        .andExpect(status().isNotFound()) 
        .andExpect(jsonPath("$.errors").value("Transaction Items with ID " + 1L + " was not found"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("should return error forbiddenrequest when request Transaction is not pending")
    public void refundTransactionItemAPI_nonPendingTrx_shouldReturnErrorForbiddenRequest() throws Exception{
        UUID transactionId = UUID.randomUUID();
        when(transactionItemService.refundTransactionItem(eq(transactionId), any(ItemRefundRequest.class))).thenThrow(new ForbiddenRequestException("Couldn't delete the item because Transaction Status is not PENDING(CART)"));

        mockMvc.perform(
            post("/api/transactions/" + transactionId.toString() + "/items/" + transactionItemResponse.id().toString() + "/refund")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .param("quantity", "5")
            .param("productId", "1")
        )
        .andDo(print()) 
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").value("Couldn't delete the item because Transaction Status is not PENDING(CART)"))
        .andExpect(jsonPath("$.data").isEmpty());
    }
}
