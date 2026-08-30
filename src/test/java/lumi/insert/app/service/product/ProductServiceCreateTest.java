package lumi.insert.app.service.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lumi.insert.app.core.entity.Product;
import lumi.insert.app.dto.request.ProductCreateRequest; 
import lumi.insert.app.dto.response.ProductResponse;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.NotFoundEntityException;
import org.springframework.cache.Cache;

public class ProductServiceCreateTest extends BaseProductServiceTest{
    
    @Test
    @DisplayName("Should return ProductResponse DTO when creating product without category is successful")
    public void createProduct_validUncategorizedRequest_returnProductResponseDTO(){
        ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
        .name("NIKE Flyway 3")
        .basePrice(BigDecimal.valueOf(100L))
        .sellPrice(BigDecimal.valueOf(120L))
        .categoryId(null)
        .stockQuantity(BigDecimal.valueOf(3L))
        .build();

        ProductResponse createdProduct = productService.createProduct(productCreateRequest);

        assertEquals("NIKE Flyway 3", createdProduct.name());
        assertEquals(BigDecimal.valueOf(100L).longValue(), createdProduct.basePrice().longValue());
        assertEquals(BigDecimal.valueOf(120L).longValue(), createdProduct.sellPrice().longValue());
        assertEquals(BigDecimal.valueOf(3L).longValue(), createdProduct.stockQuantity().longValue());
        assertEquals(BigDecimal.valueOf(0L).longValue(), createdProduct.stockMinimum().longValue());
        assertNotNull(createdProduct.id());
        assertNull(createdProduct.category());
        assertNotNull(createdProduct.createdAt());
        assertNotNull(createdProduct.updatedAt());

        Product searchedProduct = productRepository.findById(createdProduct.id()).orElseThrow(() -> new InvalidParameterException(""));

        assertEquals(createdProduct.name(), searchedProduct.getName());
        assertEquals(createdProduct.category(), searchedProduct.getCategory());
    }

    @Test
    @DisplayName("Should throw DuplicateEntityException when product name already exists")
    public void createProduct_existingName_throwDuplicateEntityException() {
        ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        when(productRepositoryMock.existsByName("NIKE Jordan Low 3")).thenReturn(true);

        assertThrows(DuplicateEntityException.class, () -> productServiceMock.createProduct(productCreateRequest));
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when provided category ID does not exist")
    public void createProduct_invalidCategoryId_throwNotFoundEntityException() {
        ProductCreateRequest productCreateRequest = ProductCreateRequest.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .categoryId(12L)
        .build();

        when(productRepositoryMock.existsByName("NIKE Jordan Low 3")).thenReturn(false);
        when(categoryRepositoryMock.findById(12L)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> productServiceMock.createProduct(productCreateRequest));
    }

    @Test
    @DisplayName("Should put cache after product created")
    public void createProduct_putCache_returnCache(){
        Cache cachedProductResponse = cacheManager.getCache("products");
        Cache cachedProductFirstPage = cacheManager.getCache("products:first-page");

        ProductResponse mockResponse = ProductResponse.builder()
            .id(1L)
            .name("Shoes")
            .build();

        cachedProductResponse.put(mockResponse.id(), mockResponse);
        cachedProductFirstPage.put("random", "random");

        ProductCreateRequest request = ProductCreateRequest.builder()
            .name("Shirt")
            .basePrice(BigDecimal.ZERO)
            .sellPrice(BigDecimal.ZERO)
            .stockMinimum(BigDecimal.ZERO)
            .stockQuantity(BigDecimal.ZERO)
            .build();

        ProductResponse product = productService.createProduct(request);

        assertNotNull(cachedProductResponse.get(product.getId()));
        ProductResponse cached = (ProductResponse) cachedProductResponse.get(product.getId()).get();

        assertEquals(product.name(), cached.name());
        assertNull(cachedProductFirstPage.get("random"));

    }
}
