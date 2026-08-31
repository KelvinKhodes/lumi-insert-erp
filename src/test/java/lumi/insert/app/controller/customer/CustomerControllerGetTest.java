package lumi.insert.app.controller.customer;

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

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.CustomerGetByFilter;
import lumi.insert.app.dto.request.CustomerGetNameRequest;
import lumi.insert.app.dto.response.CustomerNameResponse;
import lumi.insert.app.dto.response.CustomerResponse;
import lumi.insert.app.exception.NotFoundEntityException;

public class CustomerControllerGetTest extends BaseCustomerControllerTest{
    
    @Test
    void getCustomer_foundEntity_returnOKAndDTO() throws Exception{
        when(customerService.getCustomer(any(UUID.class))).thenReturn(customerDetailResponse);

        mockMvc.perform(
            get("/api/customers/" + customerDetailResponse.id())
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(customerDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(customerService, times(1)).getCustomer(argThat(arg -> arg.equals(customerDetailResponse.id())));
    }

    @Test
    void getCustomer_invalidUUID_returnBadRequest() throws Exception{ 
        mockMvc.perform(
            get("/api/customers/" + 123)
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void getCustomer_notFoundEntity_returnNotFound() throws Exception{
        when(customerService.getCustomer(any(UUID.class))).thenThrow(new NotFoundEntityException("Customer with id " + customerDetailResponse.id() + " is not found"));

        mockMvc.perform(
            get("/api/customers/" + customerDetailResponse.id())
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Customer with id " + customerDetailResponse.id() + " is not found"));

        verify(customerService, times(1)).getCustomer(argThat(arg -> arg.equals(customerDetailResponse.id())));
    }

    @Test
    void getCustomers_noFilterFoundEntity_returnOKAndListDTO() throws Exception{
        
        when(customerService.getCustomers(any(CustomerGetByFilter.class))).thenReturn(new SliceIndex<>(sliceCustomerResponse));

        mockMvc.perform(
            get("/api/customers" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(customerDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(customerService, times(1)).getCustomers(argThat(arg -> arg.getEmail() == null && arg.getMinTotalPaid().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void getCustomers_FilterFoundEntity_returnOKAndListDTO() throws Exception{
        
        when(customerService.getCustomers(any(CustomerGetByFilter.class))).thenReturn(new SliceIndex<>(sliceCustomerResponse));

        mockMvc.perform(
            get("/api/customers?name=test ger" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(customerDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(customerService, times(1)).getCustomers(argThat(arg -> arg.getName().equals("test ger")));
    }

    @Test
    void getCustomers_FilterNotFound_returnEmptyList() throws Exception{
        when(customerService.getCustomers(any(CustomerGetByFilter.class))).thenReturn(new SliceIndex<>(new SliceImpl<CustomerResponse>(List.of())));

        mockMvc.perform(
            get("/api/customers?email=ajak12@gmail.com" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0)) 
        .andExpect(jsonPath("$.data.empty").value(true)) 
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(customerService, times(1)).getCustomers(argThat(arg -> arg.getEmail().equals("ajak12@gmail.com")));
    }

    @Test
    void getCustomers_FilterBadRequest_returnBadRequest() throws Exception{
        mockMvc.perform(
            get("/api/customers?email=notemail" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data").isEmpty()) 
        .andExpect(jsonPath("$.errors").value("email doesn't meet the email format"));
    }

    @Test
    void searchCustomerNamesAPI_FilterFoundEntity_returnOKAndListDTO() throws Exception{
        when(customerService.searchCustomerNames(any(CustomerGetNameRequest.class))).thenReturn(new SliceIndex<>(sliceNames));

        mockMvc.perform(
            get("/api/customers/searchName?name=test ger" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(customerDetailResponse.id().toString()))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(customerService, times(1)).searchCustomerNames(argThat(arg -> arg.getName().equals("test ger")));
    }

    @Test
    void searchCustomerNamesAPI_filterNotFoundEntity_returnOKAndListDTO() throws Exception{
        when(customerService.searchCustomerNames(any(CustomerGetNameRequest.class))).thenReturn(new SliceIndex<>(new SliceImpl<CustomerNameResponse>(List.of())));

        mockMvc.perform(
            get("/api/customers/searchName?name=test ger" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.empty").value(true))
        .andExpect(jsonPath("$.errors").isEmpty());

        verify(customerService, times(1)).searchCustomerNames(argThat(arg -> arg.getName().equals("test ger")));
    }

    @Test
    void searchCustomerNamesAPI_FilterBadRequest_returnBadRequest() throws Exception{
        mockMvc.perform(
            get("/api/customers/searchName?name=t" )
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("Request length cannot be lesser than 3"));
    }

    
}
