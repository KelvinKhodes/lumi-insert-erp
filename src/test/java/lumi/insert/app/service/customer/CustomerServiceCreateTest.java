package lumi.insert.app.service.customer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.dto.request.CustomerCreateRequest;
import lumi.insert.app.dto.response.CustomerDetailResponse;
import lumi.insert.app.exception.DuplicateEntityException;

public class CustomerServiceCreateTest extends BaseCustomerServiceTest{
    
    @Test
    @DisplayName("should rerutn Customer Detail DTO when create entity success")
    void createCustomer_validReq_ReturnDTO(){
        when(customerRepository.existsByName(anyString())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).then(arg -> arg.getArgument(0));

        CustomerCreateRequest request = CustomerCreateRequest.builder()
        .name(setupCustomer.getName())
        .contact(setupCustomer.getContact())
        .shippingAddress(setupCustomer.getShippingAddress())
        .build();

        CustomerDetailResponse customer = customerServiceMock.createCustomer(request);
        assertNull(customer.email());
        assertEquals(setupCustomer.getEmail(), customer.email());
        verify(customerRepository, times(1)).save(argThat(arg -> arg.getName().equals(setupCustomer.getName())));
    }

    @Test
    @DisplayName("should rerutn Customer Detail DTO when create entity success")
    void createCustomer_nameDuplicate_ThrowDuplicate(){
        when(customerRepository.existsByName(anyString())).thenReturn(true); 

        CustomerCreateRequest request = CustomerCreateRequest.builder()
        .name(setupCustomer.getName())
        .contact(setupCustomer.getContact())
        .shippingAddress(setupCustomer.getShippingAddress())
        .build();

        assertThrows(DuplicateEntityException.class, () -> customerServiceMock.createCustomer(request)); 
    }
}
