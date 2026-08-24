package lumi.insert.app.service.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lumi.insert.app.dto.request.SupplierUpdateRequest;
import lumi.insert.app.dto.response.SupplierDetailResponse;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.NotFoundEntityException; 

public class SupplierServiceEditTest extends BaseSupplierServiceTest{
    
    @Test
    @DisplayName("Should return new updated Supplier when update success")
    void updateSupplier_validRequest_ReturnUpdatedDTO(){
        when(supplierRepository.existsByName(anyString())).thenReturn(false);
        when(supplierRepository.findById(setupSupplier.getId())).thenReturn(Optional.of(setupSupplier));

        SupplierUpdateRequest request = SupplierUpdateRequest.builder()
        .name("new Name LTE")
        .isActive(false)
        .build();

        SupplierDetailResponse updatedSupplier = supplierServiceMock.updateSupplier(setupSupplier.getId(), request);
        assertEquals(request.getName(), updatedSupplier.name());
        assertEquals(setupSupplier.getId(), updatedSupplier.id());
        assertFalse( updatedSupplier.isActive());
    }

    @Test
    @DisplayName("Should throw NotFoundEntity Exc when requested Supplier is not found")
    void updateSupplier_notFound_throwNotFound(){
        when(supplierRepository.findById(setupSupplier.getId())).thenReturn(Optional.empty());

        SupplierUpdateRequest request = SupplierUpdateRequest.builder()
        .name("new Name LTE")
        .isActive(false)
        .build();

        assertThrows(NotFoundEntityException.class, () -> supplierServiceMock.updateSupplier(setupSupplier.getId(), request)) ;
    }

    @Test
    @DisplayName("Should thorw DuplicateEntityException Exc when requested update name is exists")
    void updateSupplier_duplicateEntity_throwDuplicate(){
        when(supplierRepository.findById(setupSupplier.getId())).thenReturn(Optional.of(setupSupplier));
        when(supplierRepository.existsByName("new Name LTE")).thenReturn(true);

        SupplierUpdateRequest request = SupplierUpdateRequest.builder()
        .name("new Name LTE")
        .isActive(false)
        .build();

        assertThrows(DuplicateEntityException.class, () -> supplierServiceMock.updateSupplier(setupSupplier.getId(), request)) ;
    }
}
