package lumi.insert.app.service.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import lumi.insert.app.core.entity.CustomerPicture;
import lumi.insert.app.core.entity.nondatabase.CloudinaryResponse;
import lumi.insert.app.dto.request.CustomerUpdateRequest;
import lumi.insert.app.dto.response.CustomerDetailResponse;
import lumi.insert.app.exception.DatabaseInternalException;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.StorageActionException;

public class CustomerServiceEditTest extends BaseCustomerServiceTest{
    
    @Test
    @DisplayName("Should return new updated Customer when update success")
    void updateCustomer_validRequest_ReturnUpdatedDTO(){
        when(customerRepository.existsByName(anyString())).thenReturn(false);
        when(customerRepository.findById(setupCustomer.getId())).thenReturn(Optional.of(setupCustomer));

        CustomerUpdateRequest request = CustomerUpdateRequest.builder()
        .name("new Name LTE")
        .isActive(false)
        .build();

        CustomerDetailResponse updatedCustomer = customerServiceMock.updateCustomer(setupCustomer.getId(), request);
        assertEquals(request.getName(), updatedCustomer.name());
        assertEquals(setupCustomer.getId(), updatedCustomer.id());
        assertFalse( updatedCustomer.isActive());
    }

    @Test
    @DisplayName("Should thorw NotFoundEntity Exc when requested customer is not found")
    void updateCustomer_notFound_throwNotFound(){
        when(customerRepository.findById(setupCustomer.getId())).thenReturn(Optional.empty());

        CustomerUpdateRequest request = CustomerUpdateRequest.builder()
        .name("new Name LTE")
        .isActive(false)
        .build();

        assertThrows(NotFoundEntityException.class, () -> customerServiceMock.updateCustomer(setupCustomer.getId(), request)) ;
    }

    @Test
    @DisplayName("Should thorw DuplicateEntityException Exc when requested update name is exists")
    void updateCustomer_duplicateEntity_throwDuplicate(){
        when(customerRepository.findById(setupCustomer.getId())).thenReturn(Optional.of(setupCustomer));
        when(customerRepository.existsByName("new Name LTE")).thenReturn(true);

        CustomerUpdateRequest request = CustomerUpdateRequest.builder()
        .name("new Name LTE")
        .isActive(false)
        .build();

        assertThrows(DuplicateEntityException.class, () -> customerServiceMock.updateCustomer(setupCustomer.getId(), request)) ;
    }

    @Test
    void addCustomerPicture_validRequest() throws IOException{
        CloudinaryResponse cloudinaryResponse = CloudinaryResponse.builder()
        .secureUrl("testUrl.test")
        .publicId("id123")
        .build();
        
        MultipartFile[] files = {new MockMultipartFile("test", "faa".getBytes())};
        when(customerRepository.findById(setupCustomer.getId())).thenReturn(Optional.of(setupCustomer));
        when(storageService.uploadImageSync(any(), any(), any())).thenReturn(cloudinaryResponse);
        assertTrue(customerServiceMock.addCustomerPicture(setupCustomer.getId(), files));

        verify(storageService, times(1)).uploadImageSync(argThat(arg -> arg.length > 0), any(), eq("customer"));
        verify(customerPictureRepository, times(1)).saveAll(argThat(arg -> ((List<CustomerPicture>) arg).getFirst().getPictureUrl().equals(cloudinaryResponse.getSecureUrl())));

        List<String> pictureUrl = setupCustomer.getPictureUrl();
        assertEquals(1, pictureUrl.size());
        assertEquals(cloudinaryResponse.getSecureUrl(), pictureUrl.getFirst());
    }

    @Test
    void addCustomerPicture_failToUpload() throws IOException{ 
        MultipartFile[] files = {new MockMultipartFile("test", "faa".getBytes())};
        when(customerRepository.findById(setupCustomer.getId())).thenReturn(Optional.of(setupCustomer));
        when(storageService.uploadImageSync(any(), any(), any())).thenThrow(IOException.class);
        assertThrows(StorageActionException.class, () -> customerServiceMock.addCustomerPicture(setupCustomer.getId(), files));
   
        List<String> pictureUrl = setupCustomer.getPictureUrl();
        assertEquals(0, pictureUrl.size()); 
    }

    @Test
    void addCustomerPicture_failToSaveDB() throws IOException{
        CloudinaryResponse cloudinaryResponse = CloudinaryResponse.builder()
        .secureUrl("testUrl.test")
        .publicId("id123")
        .build();
        
        MultipartFile[] files = {new MockMultipartFile("test", "faa".getBytes())};
        when(customerRepository.findById(setupCustomer.getId())).thenReturn(Optional.of(setupCustomer));
        when(storageService.uploadImageSync(any(), any(), any())).thenReturn(cloudinaryResponse);
        when(customerPictureRepository.saveAll(any())).thenThrow(DataIntegrityViolationException.class);
        assertThrows(DatabaseInternalException.class, () -> customerServiceMock.addCustomerPicture(setupCustomer.getId(), files));
    
    }

    @Test
    void addCustomerPicture_notFound() throws IOException{ 
        MultipartFile[] files = {new MockMultipartFile("test", "faa".getBytes())};
        when(customerRepository.findById(setupCustomer.getId())).thenReturn(Optional.empty());  
        assertThrows(NotFoundEntityException.class, () -> customerServiceMock.addCustomerPicture(setupCustomer.getId(), files));
    }
}
