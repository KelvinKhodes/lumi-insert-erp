package lumi.insert.app.controller.supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test; 
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;

import com.github.f4b6a3.uuid.UuidCreator;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.request.SupplierGetNameRequest;
import lumi.insert.app.dto.response.SupplierDetailResponse;
import lumi.insert.app.dto.response.SupplierNameResponse;
import lumi.insert.app.exception.NotFoundEntityException; 

public class SupplierControllerGetTest extends BaseSupplierControllerTest{
    
    @Test
    void getSupplier_foundEntity_returnOKAndDTO() throws Exception{
        when(supplierService.getSupplier(any(UUID.class))).thenReturn(supplierDetailResponse);

        mockMvc.perform(
            get("/api/suppliers/" + supplierDetailResponse.id())
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(supplierDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(supplierService, times(1)).getSupplier(argThat(arg -> arg.equals(supplierDetailResponse.id())));
    }

    @Test
    void getSupplier_invalidUUID_returnBadRequest() throws Exception{ 
        mockMvc.perform(
            get("/api/suppliers/" + 123)
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void getSupplier_notFoundEntity_returnNotFound() throws Exception{
        when(supplierService.getSupplier(any(UUID.class))).thenThrow(new NotFoundEntityException("Supplier with id " + supplierDetailResponse.id() + " is not found"));

        mockMvc.perform(
            get("/api/suppliers/" + supplierDetailResponse.id())
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Supplier with id " + supplierDetailResponse.id() + " is not found"));

        verify(supplierService, times(1)).getSupplier(argThat(arg -> arg.equals(supplierDetailResponse.id())));
    }

    @Test
    void getSuppliers_noFilterFoundEntity_returnOKAndListDTO() throws Exception{
        
        when(supplierService.getSuppliers(any(SupplierGetByFilter.class))).thenReturn(new SliceIndex<>(sliceSupplierResponse));

        mockMvc.perform(
            get("/api/suppliers" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(supplierDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(supplierService, times(1)).getSuppliers(argThat(arg -> arg.getEmail() == null && arg.getMinTotalPaid().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void getSuppliers_FilterFoundEntity_returnOKAndListDTO() throws Exception{
        
        when(supplierService.getSuppliers(any(SupplierGetByFilter.class))).thenReturn(new SliceIndex<>(sliceSupplierResponse));

        mockMvc.perform(
            get("/api/suppliers?name=test ger" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(supplierDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(supplierService, times(1)).getSuppliers(argThat(arg -> arg.getName().equals("test ger")));
    }

    @Test
    void getSuppliers_FilterNotFound_returnEmptyList() throws Exception{
        when(supplierService.getSuppliers(any(SupplierGetByFilter.class))).thenReturn(new SliceIndex<>(new SliceImpl<SupplierDetailResponse>(List.of())));

        mockMvc.perform(
            get("/api/suppliers?email=ajak12@gmail.com" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0)) 
        .andExpect(jsonPath("$.data.empty").value(true)) 
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(supplierService, times(1)).getSuppliers(argThat(arg -> arg.getEmail().equals("ajak12@gmail.com")));
    }

    @Test
    void getSuppliers_FilterBadRequest_returnBadRequest() throws Exception{
        mockMvc.perform(
            get("/api/suppliers?email=notemail" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data").isEmpty()) 
        .andExpect(jsonPath("$.errors").value("email doesn't meet the email format"));
    }

    @Test
    void searchSupplierNamesAPI_FilterFoundEntity_returnOKAndListDTO() throws Exception{
        when(supplierService.searchSupplierNames(any(SupplierGetNameRequest.class))).thenReturn(new SliceIndex<>(sliceNames));

        mockMvc.perform(
            get("/api/suppliers/searchName?name=test ger" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(supplierDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(supplierService, times(1)).searchSupplierNames(argThat(arg -> arg.getName().equals("test ger")));
    }

    @Test
    void searchSupplierNamesAPI_filterNotFoundEntity_returnOKAndListDTO() throws Exception{
        when(supplierService.searchSupplierNames(any(SupplierGetNameRequest.class))).thenReturn(new SliceIndex<>(new SliceImpl<SupplierNameResponse>(List.of())));

        mockMvc.perform(
            get("/api/suppliers/searchName?name=test ger" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.empty").value(true))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(supplierService, times(1)).searchSupplierNames(argThat(arg -> arg.getName().equals("test ger")));
    }

    @Test
    void searchSupplierNamesAPI_checkLastId_returnOKAndListDTO() throws Exception{
        when(supplierService.searchSupplierNames(any(SupplierGetNameRequest.class))).thenReturn(new SliceIndex<>(new SliceImpl<SupplierNameResponse>(List.of())));
        UUID timeOrderedEpochFast = UuidCreator.getTimeOrderedEpochFast();
        mockMvc.perform(
            get("/api/suppliers/searchName?name=test ger&lastId=" + timeOrderedEpochFast)
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.empty").value(true))
        .andExpect(jsonPath("$.errors").isEmpty()); 
        verify(supplierService, times(1)).searchSupplierNames(argThat(arg -> arg.getLastId().equals(timeOrderedEpochFast)));
    }

    @Test
    void searchSupplierNamesAPI_FilterBadRequest_returnBadRequest() throws Exception{
        mockMvc.perform(
            get("/api/suppliers/searchName?name=t" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Request length cannot be lesser than 3"));
    }

    
}
