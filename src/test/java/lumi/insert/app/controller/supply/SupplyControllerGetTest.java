package lumi.insert.app.controller.supply;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any; 
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.github.f4b6a3.uuid.UuidCreator;

import lumi.insert.app.core.entity.nondatabase.SupplyStatus;
import lumi.insert.app.dto.request.SupplyGetByFilter;
import lumi.insert.app.dto.response.ProductName;
import lumi.insert.app.dto.response.SupplyDetailResponse;
import lumi.insert.app.dto.response.SupplyItemResponse;
import lumi.insert.app.dto.response.SupplyResponse;
import lumi.insert.app.exception.NotFoundEntityException; 

public class SupplyControllerGetTest extends BaseSupplyControllerTest{
    
    @Test
    @DisplayName("should return Supply Response when request Trx id is valid")
    public void getSupplyAPI_validId_shouldReturnEntity() throws Exception{
        List<SupplyItemResponse> items = new ArrayList<>();
        SupplyItemResponse supplyItemResponse = new SupplyItemResponse(UuidCreator.getTimeOrderedEpochFast(), new ProductName(1L, "Product", BigDecimal.ZERO), BigDecimal.valueOf(10L), BigDecimal.valueOf(10L), null);
        items.add(supplyItemResponse);
        items.add(supplyItemResponse);

        SupplyDetailResponse response = new SupplyDetailResponse(supplyResponse.id(), supplyResponse.invoiceId(), null, null, items, null, null, null, null, null, null, null, null, null, null, null, null, null);

        when(supplyService.getSupply(any(UUID.class))).thenReturn(response);
        mockMvc.perform(
            get("/api/supplies/" + supplyResponse.id().toString())
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.supplyItems.length()").value(items.size()))
        .andExpect(jsonPath("$.data.supplyItems[0].id").value(supplyItemResponse.id().toString()))
        .andExpect(jsonPath("$.data.supplyItems[0].product.name").value("Product"))
        .andExpect(jsonPath("$.data.id").value(supplyResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("should return Supply Response when request Trx id is invalid")
    public void getSupplyAPI_invalidId_shouldReturnErrorsOfNotFound() throws Exception{
        when(supplyService.getSupply(any(UUID.class))).thenThrow(new NotFoundEntityException("Supply with ID 1 was not found"));
        mockMvc.perform(
            get("/api/supplies/" + supplyResponse.id().toString())
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors").value("Supply with ID 1 was not found")) 
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("should return errors of invalid method when request Trx id is not UUID")
    public void getSupplyAPI_missMatch_shouldReturnErrors() throws Exception{ 
        mockMvc.perform(
            get("/api/supplies/" + true)
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").isNotEmpty()) 
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("should return Slice of Supply Response")
    public void getSuppliesAPI_filterRequest_shouldReturnSliceOfEntity() throws Exception{
        Slice<SupplyResponse> slice = new SliceImpl<>(List.of(supplyResponse));
        when(supplyService.searchSuppliesByRequests(any(SupplyGetByFilter.class))).thenReturn(slice);

        mockMvc.perform(
            get("/api/supplies/search?status=UNPAID")
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1)) 
        .andExpect(jsonPath("$.data.content[0].id").value(supplyResponse.id().toString())) 
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(supplyService).searchSuppliesByRequests(argThat(req -> 
        req.getStatus() == SupplyStatus.UNPAID &&
        req.getMinCreatedAt() == null
        ));
    }

    @Test
    @DisplayName("should return Slice < Empty of Supply Response")
    public void getSuppliesAPI_emptyData_shouldReturnSliceOfEntity() throws Exception{
        Slice<SupplyResponse> slice = new SliceImpl<>(List.of());
        when(supplyService.searchSuppliesByRequests(any(SupplyGetByFilter.class))).thenReturn(slice);

        mockMvc.perform(
            get("/api/supplies/search?status=UNPAID")
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0)) 
        .andExpect(jsonPath("$.data.content").isEmpty()) 
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("should return errors of missmatch when request parameter invalid")
    public void getSuppliesAPI_emptyData_shouldReturnErrorsMissMatch() throws Exception{
        Slice<SupplyResponse> slice = new SliceImpl<>(List.of());
        when(supplyService.searchSuppliesByRequests(any(SupplyGetByFilter.class))).thenReturn(slice);

        mockMvc.perform(
            get("/api/supplies/search?minCreatedAt=" + true)
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty()) 
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("should return errors of min constraint when request parameter invalid")
    public void getSuppliesAPI_minusParamFilter_shouldReturnErrorsMissMatch() throws Exception{
        Slice<SupplyResponse> slice = new SliceImpl<>(List.of());
        when(supplyService.searchSuppliesByRequests(any(SupplyGetByFilter.class))).thenReturn(slice);

        mockMvc.perform(
            get("/api/supplies/search?minTotalItems=" + -2)
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty()) 
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("should return errors of pattern constraint when request parameter invalid")
    public void getSuppliesAPI_sortBySpec_shouldReturnErrorsMissMatch() throws Exception{
        Slice<SupplyResponse> slice = new SliceImpl<>(List.of());
        when(supplyService.searchSuppliesByRequests(any(SupplyGetByFilter.class))).thenReturn(slice);

        mockMvc.perform(
            get("/api/supplies/search?sortBy=" + -2)
            .accept(MediaType.APPLICATION_JSON_VALUE) 
        )
        .andDo(print()) 
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty()) 
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 200 http status and pdf content when request valid")
    public void exportSupplyAPI_validRequest_return400StatusAndPdf() throws Exception{
        ByteArrayInputStream bais = new ByteArrayInputStream("pdfBinary".getBytes());

        UUID supplyId = UuidCreator.getTimeOrderedEpochFast();
         
        when(supplyService.getSupply(supplyId)).thenReturn(new SupplyDetailResponse(supplyId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        when(pdfService.exportSupplyWithItems(any(SupplyDetailResponse.class))).thenReturn(bais);

        MvcResult result = mockMvc.perform(
                get("/api/supplies/" + supplyId + "/pdf")
                .accept(MediaType.APPLICATION_PDF)
            )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
        .andReturn();

        assertTrue(result.getResponse().getContentAsByteArray().length > 0);
        
        verify(pdfService, times(1)).exportSupplyWithItems(argThat(arg -> arg.id() == supplyId));
    }

    @Test
    @DisplayName("Should return 200 http status and xlsx content when request valid")
    public void exportSuppliesHistoryAPI_validRequest_return200StatusAndXlsx() throws Exception{ 
  
        SupplyResponse supplyResponse = new SupplyResponse(UuidCreator.getTimeOrderedEpochFast(), 
            "INV-012", 
            null ,
            "TEST LTE.", 
            2L, 
            BigDecimal.valueOf(3L), 
            BigDecimal.valueOf(4L), 
            BigDecimal.valueOf(5L), 
            BigDecimal.valueOf(6L), 
            BigDecimal.valueOf(7L), 
            BigDecimal.valueOf(8L),
             BigDecimal.valueOf(9L),
             BigDecimal.valueOf(10L), 
             SupplyStatus.COMPLETE, 
             null, 
             null, 
             LocalDateTime.now()
            );

        when(supplyService.searchSuppliesByRequests(any())).thenReturn(new SliceImpl<>(List.of(supplyResponse)));

         mockMvc.perform(
                get("/api/supplies/history/export?status=COMPLETE")
                .accept(MediaType.APPLICATION_XML_VALUE)
            )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(content().contentType(MediaType.APPLICATION_XML_VALUE));
        
        verify(supplyService, times(1)).searchSuppliesByRequests(argThat(arg -> arg.getStatus() == SupplyStatus.COMPLETE && arg.getSize() == 99999000));
        verify(xlsxService, times(1)).exportSupplies(argThat(arg -> arg.getFirst().invoiceId() == supplyResponse.invoiceId()), any());
    }

    @Test
    @DisplayName("Should return 200 http status and xlsx content when request valid")
    public void exportSuppliesHistoryAPI_invalidType_return400Status() throws Exception{ 
         mockMvc.perform(
                get("/api/supplies/history/export?status=TRUE")
                .accept(MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE)
            )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty()) 
        .andExpect(jsonPath("$.errors").isNotEmpty()); 
    }
}
