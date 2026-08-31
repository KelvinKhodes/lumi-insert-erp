package lumi.insert.app.controller.transactionpayment;
 
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;

import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.TransactionPaymentGetByFilter;
import lumi.insert.app.dto.response.TransactionPaymentResponse; 
import lumi.insert.app.exception.NotFoundEntityException;

public class TransactionPaymentControllerGetTest extends BaseTransactionPaymentControllerTest{
    
    @Test
    @DisplayName("Should return transactionPayment DTO when found")
    void getTransactionPaymentAPI_validRequest_shouldReturnEntityDTO() throws Exception{
        when(transactionPaymentService.getTransactionPayment(transactionPaymentResponse.id())).thenReturn(transactionPaymentResponse);

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments/" + transactionPaymentResponse.id().toString())
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data.id").value(transactionPaymentResponse.id().toString()));
    }

    @Test
    @DisplayName("Should return error missmatch type when request id is not UUID")
    void getTransactionPaymentAPI_missMatch_shouldReturnErrorMissMatch() throws Exception{
        when(transactionPaymentService.getTransactionPayment(transactionPaymentResponse.id())).thenReturn(transactionPaymentResponse);

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments/" + true)
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").isNotEmpty()); 
    }

    @Test
    @DisplayName("Should return error NotFound when request transaction payment not found")
    void getTransactionPaymentAPI_notFound_shouldReturnErrorNotFound() throws Exception{
        when(transactionPaymentService.getTransactionPayment(transactionPaymentResponse.id())).thenThrow(new NotFoundEntityException("Transaction Payment with ID " + 1L + " was not found"));

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments/" + transactionPaymentResponse.id())
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Transaction Payment with ID " + 1L + " was not found")); 
    }

    @Test
    @DisplayName("Should return slice of transactionPayment DTO when transaction found/valid")
    void getTransactionPaymentsAPI_validRequest_shouldReturnSliceEntityDTO() throws Exception{
        Slice<TransactionPaymentResponse> slice = new SliceImpl<TransactionPaymentResponse>(List.of(transactionPaymentResponse));
        when(transactionPaymentService.getTransactionPaymentsByTransactionId(transactionPaymentResponse.transactionId(), PaginationRequest.builder().build())).thenReturn(slice);

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data.content[0].id").value(transactionPaymentResponse.id().toString()))
        .andExpect(jsonPath("$.data.content.length()").value(slice.getNumberOfElements()));
        verify(transactionPaymentService, times(1)).getTransactionPaymentsByTransactionId(transactionPaymentResponse.transactionId(), PaginationRequest.builder().build());
    }

    @Test
    @DisplayName("Should return slice with 0 data when transaction found/valid but no payment")
    void getTransactionPaymentsAPI_validRequestCaseNoPayment_shouldReturnSliceEntityDTO() throws Exception{
        Slice<TransactionPaymentResponse> slice = new SliceImpl<TransactionPaymentResponse>(List.of());
        when(transactionPaymentService.getTransactionPaymentsByTransactionId(transactionPaymentResponse.transactionId(), PaginationRequest.builder().build())).thenReturn(slice);

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(slice.getNumberOfElements()));
        verify(transactionPaymentService, times(1)).getTransactionPaymentsByTransactionId(transactionPaymentResponse.transactionId(), PaginationRequest.builder().build());
    }

    @Test
    @DisplayName("Should return slice of transactionPayment DTO when transaction found/valid")
    void searchTransactionPaymentsFilterAPI_validRequest_shouldReturnSliceEntityDTO() throws Exception{
        Slice<TransactionPaymentResponse> slice = new SliceImpl<TransactionPaymentResponse>(List.of(transactionPaymentResponse));
        when(transactionPaymentService.getTransactionPaymentsByRequests(TransactionPaymentGetByFilter.builder().transactionId(transactionPaymentResponse.transactionId()).minTotalPayment(BigDecimal.valueOf(5000L)).build())).thenReturn(new SliceIndex<>(slice));

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments/filter?minTotalPayment=5000")
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.data.content[0].id").value(transactionPaymentResponse.id().toString()))
        .andExpect(jsonPath("$.data.content.length()").value(slice.getNumberOfElements())); 
    }

    @Test
    @DisplayName("Should return error missmatch when request param type missmatch")
    void searchTransactionPaymentsFilterAPI_missmatchParam_shouldReturnSliceEntityDTO() throws Exception{
        Slice<TransactionPaymentResponse> slice = new SliceImpl<TransactionPaymentResponse>(List.of(transactionPaymentResponse));
        when(transactionPaymentService.getTransactionPaymentsByRequests(TransactionPaymentGetByFilter.builder().transactionId(transactionPaymentResponse.transactionId()).minTotalPayment(BigDecimal.valueOf(5000L)).build())).thenReturn(new SliceIndex<>(slice));

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments/filter?minTotalPayment=true")
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("Should return error badrequest  when param filter totalpayment is lower than 0")
    void searchTransactionPaymentsFilterAPI_minTotalPaymentMinus_shouldReturnSliceEntityDTO() throws Exception{
        Slice<TransactionPaymentResponse> slice = new SliceImpl<TransactionPaymentResponse>(List.of(transactionPaymentResponse));
        when(transactionPaymentService.getTransactionPaymentsByRequests(TransactionPaymentGetByFilter.builder().transactionId(transactionPaymentResponse.transactionId()).minTotalPayment(BigDecimal.valueOf(5000L)).build())).thenReturn(new SliceIndex<>(slice));

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments/filter?minTotalPayment=-5000")
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").value("minTotalPayment minimal value is 0"));
    }

    @Test
    @DisplayName("Should return error badrequest  when param filter totalpayment is lower than 0")
    void searchTransactionPaymentsFilterAPI_wrongSortByValue_shouldReturnSliceEntityDTO() throws Exception{
        Slice<TransactionPaymentResponse> slice = new SliceImpl<TransactionPaymentResponse>(List.of(transactionPaymentResponse));
        when(transactionPaymentService.getTransactionPaymentsByRequests(TransactionPaymentGetByFilter.builder().transactionId(transactionPaymentResponse.transactionId()).minTotalPayment(BigDecimal.valueOf(5000L)).build())).thenReturn(new SliceIndex<>(slice));

        mockMvc.perform(
            get("/api/transactions/" + transactionPaymentResponse.transactionId() + "/payments/filter?minTotalPayment=5000&sortBy=cc")
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").value("check documentation for sortBy specification"));
    }
}
