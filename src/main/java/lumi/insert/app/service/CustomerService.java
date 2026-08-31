package lumi.insert.app.service;

import java.util.UUID;

import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.CustomerCreateRequest;
import lumi.insert.app.dto.request.CustomerGetByFilter;
import lumi.insert.app.dto.request.CustomerGetNameRequest;
import lumi.insert.app.dto.request.CustomerUpdateRequest;
import lumi.insert.app.dto.response.CustomerDetailResponse;
import lumi.insert.app.dto.response.CustomerNameResponse;
import lumi.insert.app.dto.response.CustomerResponse;

public interface CustomerService {
    
    CustomerDetailResponse createCustomer(CustomerCreateRequest request);

    CustomerDetailResponse getCustomer(UUID id);

    SliceIndex<CustomerResponse> getCustomers(CustomerGetByFilter request);

    SliceIndex<CustomerNameResponse> searchCustomerNames(CustomerGetNameRequest request);

    CustomerDetailResponse updateCustomer(UUID id, CustomerUpdateRequest request);
    
    Boolean addCustomerPicture(UUID id, MultipartFile[] files);

}
