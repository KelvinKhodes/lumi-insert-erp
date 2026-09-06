package lumi.insert.app.service;

import java.util.List;

import org.springframework.data.domain.Slice;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.core.repository.projection.ProductOutOfStock;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.ProductCreateRequest;
import lumi.insert.app.dto.request.ProductUpdateRequest;
import lumi.insert.app.dto.request.ProductGetByFilter;
import lumi.insert.app.dto.request.ProductGetNameRequest;

import lumi.insert.app.dto.response.ProductDeleteResponse;
import lumi.insert.app.dto.response.ProductName;
import lumi.insert.app.dto.response.ProductResponse;
import lumi.insert.app.dto.response.ProductStockResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductStockResponse getProductStock(Long id);

    ProductResponse updateProduct(ProductUpdateRequest request);

    SliceIndex<ProductName> searchProductNames(ProductGetNameRequest request);

    ProductResponse getProductById(Long id);

    Slice<ProductResponse> getProducts(PaginationRequest request);

    SliceIndex<ProductResponse> getProductsByRequests(ProductGetByFilter request);

    ProductDeleteResponse deactivateProduct(Long id);

    ProductDeleteResponse activateProduct(Long id);

    List<ProductOutOfStock> getOutOfStockProducts();

    String uploadProductPictures(Long id, MultipartFile[] files);

}
