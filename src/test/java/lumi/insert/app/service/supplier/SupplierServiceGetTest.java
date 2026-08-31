package lumi.insert.app.service.supplier;
 
import static org.junit.jupiter.api.Assertions.assertEquals; 
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;

import com.github.f4b6a3.uuid.UuidCreator;

import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.request.SupplierGetNameRequest;
import lumi.insert.app.dto.response.SupplierDetailResponse;
import lumi.insert.app.dto.response.SupplierNameResponse;
import lumi.insert.app.exception.NotFoundEntityException; 

public class SupplierServiceGetTest extends BaseSupplierServiceTest{
    
    @Test
    @DisplayName("Should return Supplier detail DTO when get Supplier found")
    void getSupplier_foundEntity_returnSupplierDTO(){
        when(supplierRepository.findById(setupSupplier.getId())).thenReturn(Optional.of(setupSupplier));

        SupplierDetailResponse Supplier = supplierServiceMock.getSupplier(setupSupplier.getId());
        assertEquals(setupSupplier.getName(), Supplier.name());
    }

    @Test
    @DisplayName("Should return Supplier detail DTO when get Supplier found")
    void getSupplier_notFoundEntity_throwNotFound(){
        when(supplierRepository.findById(setupSupplier.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> supplierServiceMock.getSupplier(setupSupplier.getId())); 
    }

    @Test
    @DisplayName("Should return Supplier Name DTO when found")
    void searchProductNames_foundEntity_returnSupplierNameDTO(){
        SupplierNameResponse name = new SupplierNameResponse(setupSupplier.getId(), setupSupplier.getName());

        Slice<SupplierNameResponse> slices = new SliceImpl<>(List.of(name));
        when(supplierRepository.getByNameContainingIgnoreCaseAndIdAfter(eq("tes"),any(UUID.class), any(Pageable.class))).thenReturn(slices);

        SupplierGetNameRequest request = SupplierGetNameRequest.builder()
        .name("tes")
        .lastId(UuidCreator.getTimeOrderedEpochFast())
        .build();

        SliceIndex<SupplierNameResponse> Supplier = supplierServiceMock.searchSupplierNames(request);
        assertEquals(1, Supplier.getNumberOfElements());
        assertEquals(setupSupplier.getName(), Supplier.getContent().getFirst().name());
        verify(supplierRepository, times(1)).getByNameContainingIgnoreCaseAndIdAfter(any(), eq(request.getLastId()), argThat(arg -> arg.getPageSize() == 10));
    }

    @Test
    @DisplayName("Should return empty Supplier Name DTO when found")
    void searchProductNames_notFoundEntity_returnEmptySupplierNameDTO(){ 

        Slice<SupplierNameResponse> slices = new SliceImpl<>(List.of());
        when(supplierRepository.getByNameContainingIgnoreCaseAndIdAfter(eq("tes"),any(UUID.class), any(Pageable.class))).thenReturn(slices);

        SupplierGetNameRequest request = SupplierGetNameRequest.builder()
        .name("tes")
        .build();

        SliceIndex<SupplierNameResponse> Supplier = supplierServiceMock.searchSupplierNames(request);
        assertEquals(0, Supplier.getNumberOfElements());
        assertEquals(List.of(), Supplier.getContent());
        verify(supplierRepository, times(1)).getByNameContainingIgnoreCaseAndIdAfter(any(), eq(new UUID(0, 0)), argThat(arg -> arg.getPageSize() == 10));
    }

    @Test
    @DisplayName("Should return Slice Supplier DTO when get Supplier found")
    void getSuppliers_foundEntity_returnSliceSupplierDTO(){
        Page<Supplier> slices = new PageImpl<>(List.of(setupSupplier));
        when(supplierRepository.findAll(ArgumentMatchers.<Specification<Supplier>>any(), any(Pageable.class))).thenReturn(slices);

        SupplierGetByFilter request = SupplierGetByFilter.builder()
        .isActive(false)
        .minTotalUnpaid(BigDecimal.valueOf(1000L))
        .maxTotalUnpaid(BigDecimal.valueOf(1500L))
        .build();

        SliceIndex<SupplierDetailResponse> Supplier = supplierServiceMock.getSuppliers(request);
        assertEquals(1, Supplier.getNumberOfElements()); 
    }

    @Test
    @DisplayName("Should return Slice Supplier DTO when get Supplier found")
    void getSuppliers_notFoundEntity_returnSliceSupplierDTO(){
        Page<Supplier> slices = new PageImpl<>(List.of());
        when(supplierRepository.findAll(ArgumentMatchers.<Specification<Supplier>>any(), any(Pageable.class))).thenReturn(slices);

        SupplierGetByFilter request = SupplierGetByFilter.builder()
        .isActive(false)
        .minTotalUnpaid(BigDecimal.valueOf(1000L))
        .maxTotalUnpaid(BigDecimal.valueOf(1500L))
        .build();

        SliceIndex<SupplierDetailResponse> Supplier = supplierServiceMock.getSuppliers(request);
        assertEquals(0, Supplier.getNumberOfElements()); 
    }
}
