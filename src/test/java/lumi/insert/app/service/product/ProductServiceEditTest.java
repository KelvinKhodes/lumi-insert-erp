package lumi.insert.app.service.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import lumi.insert.app.dto.request.ProductCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lumi.insert.app.core.entity.Category;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.dto.request.ProductUpdateRequest;
import lumi.insert.app.dto.response.ProductDeleteResponse;
import lumi.insert.app.dto.response.ProductResponse;
import lumi.insert.app.exception.BoilerplateRequestException;
import lumi.insert.app.exception.NotFoundEntityException;
import org.springframework.cache.Cache;

public class ProductServiceEditTest extends BaseProductServiceTest {
    @Test
    @DisplayName("Should return updated ProductResponse DTO when update with valid ID is successful")
    public void updateProduct_validRequest_returnUpdatedProductResponseDTO(){

        Product mockProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        Product savedProduct = productRepository.save(mockProduct);

        ProductUpdateRequest productEditRequest = ProductUpdateRequest.builder()
        .id(savedProduct.getId())
        .name("NIKE Jordan Low 4")
        .basePrice(BigDecimal.valueOf(11000L))
        .sellPrice(BigDecimal.valueOf(13000L))
        .stockMinimum(BigDecimal.valueOf(2L))
        .build();

        ProductResponse editedProduct = productService.updateProduct(productEditRequest);

        assertEquals("NIKE Jordan Low 4", editedProduct.name());
        assertTrue(BigDecimal.valueOf(11000L).compareTo(editedProduct.basePrice()) == 0);
        assertTrue(BigDecimal.valueOf(13000L).compareTo(editedProduct.sellPrice()) == 0);
        assertTrue(BigDecimal.valueOf(50L).compareTo(editedProduct.stockQuantity()) == 0);
        assertTrue(BigDecimal.valueOf(2L).compareTo(editedProduct.stockMinimum()) == 0);
        assertEquals(savedProduct.getId(), editedProduct.id());
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when updating product with non-existent ID")
    public void updateProduct_idNotFound_throwNotFoundEntityException(){

        ProductUpdateRequest productEditRequest = ProductUpdateRequest.builder()
        .id(1L)
        .name("NIKE Jordan Low 4")
        .basePrice(BigDecimal.valueOf(11000L))
        .sellPrice(BigDecimal.valueOf(13000L))
        .stockMinimum(BigDecimal.valueOf(2L))
        .build();

        assertThrows(NotFoundEntityException.class, () -> productServiceMock.updateProduct(productEditRequest));
    }

    @Test
    @DisplayName("Should increment category total items when product category is updated")
    public void updateProduct_changeCategory_incrementCategoryTotalItems() {
        Category category = Category.builder()
        .name("Shoes")
        .build();

        Category savedCategory = categoryRepository.save(category);

        assertEquals(0L, savedCategory.getTotalItems());

        Product mockProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .build();

        Product savedProduct = productRepository.save(mockProduct);

        ProductUpdateRequest request = ProductUpdateRequest.builder()
        .id(savedProduct.getId())
        .categoryId(savedCategory.getId())
        .build();

        ProductResponse editProductResponse = productService.updateProduct(request);
        Category updatedCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow(() -> new IllegalArgumentException("N"));
        
        assertEquals(1L, updatedCategory.getTotalItems());
        assertEquals("NIKE Jordan Low 3", editProductResponse.name());
        assertEquals(savedProduct.getId(), editProductResponse.id());   
    }

    @Test
    @DisplayName("Should decrement category total items when product is deactivated")
    public void deactivateProduct_validId_decrementCategoryTotalItemsAndReturnResponse(){
        Category category = Category.builder()
        .name("Shoes")
        .totalItems(10L)
        .build();

        Category savedCategory = categoryRepository.save(category);

        assertEquals(10L, savedCategory.getTotalItems());

        Product mockProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .category(savedCategory)
        .build();

        Product savedProduct = productRepository.save(mockProduct);

        ProductDeleteResponse setInactiveProduct = productService.deactivateProduct(savedProduct.getId());

        Category updatedCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow(() -> new IllegalArgumentException("N"));
        assertEquals(9L, updatedCategory.getTotalItems());

        assertFalse(setInactiveProduct.isActive());
    }

    @Test
    @DisplayName("Should increment category total items when product is activated")
    public void activateProduct_validId_incrementCategoryTotalItemsAndReturnResponse(){
        Category category = Category.builder()
        .name("Shoes")
        .totalItems(10L)
        .build();

        Category savedCategory = categoryRepository.save(category);

        assertEquals(10L, savedCategory.getTotalItems());

        Product mockProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .category(savedCategory)
        .isActive(false)
        .build();

        Product savedProduct = productRepository.save(mockProduct);

        ProductDeleteResponse setInactiveProduct = productService.activateProduct(savedProduct.getId());

        Category updatedCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow(() -> new IllegalArgumentException("N"));
        assertEquals(11L, updatedCategory.getTotalItems());

        assertTrue(setInactiveProduct.isActive());
    }

    @Test
    @DisplayName("Should throw BoilerplateRequestException when activating an already active product")
    public void activateProduct_alreadyActive_throwBoilerplateRequestException(){
        Product alreadyActiveProduct = Product.builder()
        .id(90L)
        .isActive(true)
        .build();

        when(productRepositoryMock.findById(90L)).thenReturn(Optional.of(alreadyActiveProduct));
        assertThrows(BoilerplateRequestException.class, () -> productServiceMock.activateProduct(90L));
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when activating non-existent product ID")
    public void activateProduct_idNotFound_throwNotFoundEntityException(){
        when(productRepositoryMock.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundEntityException.class, () -> productServiceMock.activateProduct(1L));
    }

    @Test
    @DisplayName("Should throw BoilerplateRequestException when deactivating an already inactive product")
    public void deactivateProduct_alreadyInactive_throwBoilerplateRequestException(){
        Product alreadyInactiveProduct = Product.builder()
        .id(90L)
        .isActive(false)
        .build();

        when(productRepositoryMock.findById(90L)).thenReturn(Optional.of(alreadyInactiveProduct));
        assertThrows(BoilerplateRequestException.class, () -> productServiceMock.deactivateProduct(90L));
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when deactivating non-existent product ID")
    public void deactivateProduct_idNotFound_throwNotFoundEntityException(){
        when(productRepositoryMock.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundEntityException.class, () -> productServiceMock.deactivateProduct(1L));
    }

    @Test
    @DisplayName("Should put and override cache after product updated")
    public void updateProduct_putCache_returnCache(){
        Cache cachedProductResponse = cacheManager.getCache("products");
        Cache cachedProductFirstPage = cacheManager.getCache("products:first-page");

        Product mockProduct = Product.builder()
            .name("NIKE Jordan Low 3")
            .basePrice(BigDecimal.valueOf(10000L))
            .sellPrice(BigDecimal.valueOf(12000L))
            .stockQuantity(BigDecimal.valueOf(50L))
            .stockMinimum(BigDecimal.valueOf(5L))
            .build();

        Product savedProduct = productRepository.save(mockProduct);

        ProductResponse mockResponse = ProductResponse.builder()
            .id(savedProduct.getId())
            .name("NIKE Jordan Low 3")
            .build();

        cachedProductResponse.put(mockResponse.id(), mockResponse);
        cachedProductFirstPage.put("random", "random");

        ProductUpdateRequest productEditRequest = ProductUpdateRequest.builder()
            .id(savedProduct.getId())
            .name("NIKE Jordan Low 4")
            .basePrice(BigDecimal.valueOf(11000L))
            .sellPrice(BigDecimal.valueOf(13000L))
            .stockMinimum(BigDecimal.valueOf(2L))
            .build();

        ProductResponse editedProduct = productService.updateProduct(productEditRequest);

        assertNotNull(cachedProductResponse.get(mockResponse.getId()));
        ProductResponse cached = (ProductResponse) cachedProductResponse.get(mockResponse.getId()).get();

        assertEquals(editedProduct.name(), cached.name());
        assertNull(cachedProductFirstPage.get("random"));

    }

    @Test
    @DisplayName("Should evict products cache when activate product")
    public void activateProduct_evictAllCaches(){
        Cache cachedProductResponse = cacheManager.getCache("products");
        Cache cachedProductFirstPage = cacheManager.getCache("products:first-page");

        Category category = Category.builder()
            .name("Shoes")
            .totalItems(10L)
            .build();

        Category savedCategory = categoryRepository.save(category);

        Product mockProduct = Product.builder()
            .name("NIKE Jordan Low 3")
            .basePrice(BigDecimal.valueOf(10000L))
            .sellPrice(BigDecimal.valueOf(12000L))
            .stockQuantity(BigDecimal.valueOf(50L))
            .stockMinimum(BigDecimal.valueOf(5L))
            .category(savedCategory)
            .isActive(false)
            .build();

        Product savedProduct = productRepository.save(mockProduct);

        cachedProductResponse.put(savedProduct.getId(), "mockValue");
        cachedProductFirstPage.put("random", "random");

        ProductDeleteResponse setInactiveProduct = productService.activateProduct(savedProduct.getId());
        assertNull(cachedProductFirstPage.get("random"));
        assertNull(cachedProductResponse.get(setInactiveProduct.id()));
        assertTrue(setInactiveProduct.isActive());
    }

    @Test
    @DisplayName("Should evict products cache when deactivate product")
    public void deactivateProduct_evictAllCaches(){
        Cache cachedProductResponse = cacheManager.getCache("products");
        Cache cachedProductFirstPage = cacheManager.getCache("products:first-page");

        Category category = Category.builder()
            .name("Shoes")
            .totalItems(10L)
            .build();

        Category savedCategory = categoryRepository.save(category);

        Product mockProduct = Product.builder()
            .name("NIKE Jordan Low 3")
            .basePrice(BigDecimal.valueOf(10000L))
            .sellPrice(BigDecimal.valueOf(12000L))
            .stockQuantity(BigDecimal.valueOf(50L))
            .stockMinimum(BigDecimal.valueOf(5L))
            .category(savedCategory)
            .isActive(true)
            .build();

        Product savedProduct = productRepository.save(mockProduct);

        cachedProductResponse.put(savedProduct.getId(), "mockValue");
        cachedProductFirstPage.put("random", "random");

        ProductDeleteResponse setInactiveProduct = productService.deactivateProduct(savedProduct.getId());

        assertNull(cachedProductFirstPage.get("random"));
        assertNull(cachedProductResponse.get(savedProduct.getId()));
        assertFalse(setInactiveProduct.isActive());
    }
}
