package lumi.insert.app.service;

import java.util.UUID;

import org.springframework.data.domain.Slice;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.SupplierCreateRequest;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.request.SupplierGetNameRequest;
import lumi.insert.app.dto.request.SupplierUpdateRequest;
import lumi.insert.app.dto.response.SupplierDetailResponse;
import lumi.insert.app.dto.response.SupplierNameResponse; 

public interface SupplierService {
    
    SupplierDetailResponse createSupplier(SupplierCreateRequest request);

    SupplierDetailResponse getSupplier(UUID id);

    SliceIndex<SupplierDetailResponse> getSuppliers(SupplierGetByFilter request);

    SliceIndex<SupplierNameResponse> searchSupplierNames(SupplierGetNameRequest request);

    SupplierDetailResponse updateSupplier(UUID id, SupplierUpdateRequest request); 

}
