package lumi.insert.app.service.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lumi.insert.app.dto.request.ProductCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import lumi.insert.app.core.entity.Category;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.core.repository.projection.ProductOutOfStock;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.ProductGetByFilter;
import lumi.insert.app.dto.request.ProductGetNameRequest;
import lumi.insert.app.dto.response.ProductName;
import lumi.insert.app.dto.response.ProductResponse;
import lumi.insert.app.dto.response.ProductStockResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;


public class ProductServiceGetTest extends BaseProductServiceTest{
    
    @Test
    @DisplayName("Should return ProductStockResponse when getting stock with valid ID")
    public void getProductStock_validId_returnProductStockResponse(){
        when(productRepositoryMock.getStockById(1L)).thenReturn(Optional.of(BigDecimal.valueOf(50L)));
        ProductStockResponse productStock = productServiceMock.getProductStock(1L);

        assertTrue(BigDecimal.valueOf(50L).compareTo(productStock.stockQuantity()) == 0);
        assertEquals(1L, productStock.id());

        verify(productRepositoryMock, times(1)).getStockById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when getting stock with invalid ID")
    public void getProductStock_invalidId_throwNotFoundEntityException() {
        when(productRepositoryMock.getStockById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> productServiceMock.getProductStock(2L));

        verify(productRepositoryMock, times(1)).getStockById(2L);
    }

    @Test
    @DisplayName("Should return Slice of ProductName when searching by valid name query")
    public void searchProductNames_validQuery_returnSliceOfProductName(){
        List<ProductName> products = new ArrayList<ProductName>();

        for ( int i = 1; i <= 12; i++ ) {
            ProductName dumpProduct = new ProductName(Long.valueOf(i), "Product " + i, BigDecimal.ZERO);
 
            products.add(dumpProduct);
        }

        Slice<ProductName> productSlice = new SliceImpl<>(products);

        when(productRepositoryMock.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter(eq("Pro"), eq(1L), any(Pageable.class))).thenReturn(productSlice);

        ProductGetNameRequest request = ProductGetNameRequest.builder()
        .name("Pro")
        .page(0)
        .size(5)
        .lastId(1L)
        .build();

        SliceIndex<ProductName> allProductNames = productServiceMock.searchProductNames(request);

        assertEquals(12, allProductNames.getNumberOfElements());
        assertEquals("Product 1", allProductNames.getContent().get(0).name());
        assertEquals("Product 2", allProductNames.getContent().get(1).name());
        assertEquals("Product 3", allProductNames.getContent().get(2).name());
        assertEquals("Product 4", allProductNames.getContent().get(3).name());
        assertFalse(allProductNames.isHasNext());
    }

    @Test
    @DisplayName("Should return empty Slice when searching product name with non-matching query")
    public void searchProductNames_queryNotFound_returnEmptySlice(){

        Slice<ProductName> productSlice = new SliceImpl<>(List.of());

        when(productRepositoryMock.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter(eq("Pro"), eq(0L) , any(Pageable.class))).thenReturn(productSlice);

        ProductGetNameRequest request = ProductGetNameRequest.builder()
        .name("Pro")
        .page(0)
        .size(5)
        .build();

        SliceIndex<ProductName> allProductNames = productServiceMock.searchProductNames(request);

        assertEquals(0, allProductNames.getNumberOfElements());
        assertFalse(allProductNames.isHasNext());
        assertTrue(allProductNames.isEmpty());
    }

   @Test
    @DisplayName("Should return ProductResponse DTO when getting product by valid ID without category")
    public void getProductById_validIdNoCategory_returnProductResponseDTO() {
        Product mockProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .id(1L)
        .build();

        when(productRepositoryMock.findById(1L)).thenReturn(Optional.of(mockProduct));
        // when(categoryMapper.createDtoSimpleResponseFromCategory(any())).thenReturn(null);
        ProductResponse productById = productServiceMock.getProductById(1L);
        verify(productMapper, times(1)).createDtoResponseFromProduct(mockProduct);

        assertEquals("NIKE Jordan Low 3", productById.name());
        assertTrue(BigDecimal.valueOf(10000L).compareTo(productById.basePrice()) == 0);
        assertTrue(BigDecimal.valueOf(12000L).compareTo(productById.sellPrice()) == 0);
        assertTrue(BigDecimal.valueOf(50L).compareTo(productById.stockQuantity()) == 0);
        assertTrue(BigDecimal.valueOf(5L).compareTo(productById.stockMinimum()) == 0);
        assertEquals(1L, productById.id());
        assertNull(productById.category());
    }

    @Test
    @DisplayName("Should return ProductResponse DTO with category details when getting product by valid ID")
    public void getProductById_validIdWithCategory_returnProductResponseDTO(){
        Category mockCategory = Category.builder()
        .id(2L)
        .name("Shoes")
        .build();

        Product mockProduct = Product.builder()
        .name("NIKE Jordan Low 3")
        .basePrice(BigDecimal.valueOf(10000L))
        .sellPrice(BigDecimal.valueOf(12000L))
        .stockQuantity(BigDecimal.valueOf(50L))
        .stockMinimum(BigDecimal.valueOf(5L))
        .category(mockCategory)
        .id(1L)
        .build();

        when(productRepositoryMock.findById(1L)).thenReturn(Optional.of(mockProduct));


        ProductResponse productById = productServiceMock.getProductById(1L);

        verify(productMapper, times(1)).createDtoResponseFromProduct(mockProduct);
        assertEquals("NIKE Jordan Low 3", productById.name());
        assertTrue(BigDecimal.valueOf(10000L).compareTo(productById.basePrice()) == 0);
        assertTrue(BigDecimal.valueOf(12000L).compareTo(productById.sellPrice()) == 0);
        assertTrue(BigDecimal.valueOf(50L).compareTo(productById.stockQuantity()) == 0);
        assertTrue(BigDecimal.valueOf(5L).compareTo(productById.stockMinimum()) == 0);
        assertEquals(1L, productById.id());
        assertEquals(2L, productById.category().id());
        assertEquals("Shoes", productById.category().name());
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when getting product by invalid ID")
    public void getProductById_invalidId_throwNotFoundEntityException(){
        when(productRepositoryMock.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> productServiceMock.getProductById(1L));
    }

    @Test
    @DisplayName("Should return Slice of all ProductResponse DTOs based on pagination")
    public void getProducts_validPagination_returnSliceOfProductResponseDTO(){
        List<Product> products = new ArrayList<Product>();

        for ( int i = 1; i <= 12; i++ ) {
            final Long ids = Long.valueOf(i);
            Product dumpProduct = Product.builder()
            .id(ids)
            .name("Product " + i)
            .basePrice(BigDecimal.valueOf(1000L * i))
            .sellPrice(BigDecimal.valueOf(1200L * i))
            .stockQuantity(BigDecimal.valueOf(10L * i))
            .stockMinimum(BigDecimal.valueOf(1L * i))
            .build();

            products.add(dumpProduct);
        }

        Slice<Product> productSlice = new SliceImpl<>(products);

        when(productRepositoryMock.findAllBy(any(Pageable.class))).thenReturn(productSlice);

        PaginationRequest request = PaginationRequest.builder()
        .page(0)
        .size(12)
        .build();

        Slice<ProductResponse> allProducts = productServiceMock.getProducts(request);

        verify(productMapper, times(12)).createDtoResponseFromProduct(any(Product.class));

        assertEquals(12, allProducts.getNumberOfElements());
        assertFalse(allProducts.hasNext());

        List<ProductResponse> content = allProducts.getContent();
        content.forEach(e -> {
            assertNotNull(e.name());
            assertNotNull(e.basePrice());
            assertNotNull(e.sellPrice());
            assertNotNull(e.stockQuantity());
            assertNotNull(e.stockMinimum());
            assertNotNull(e.id());
        });
    }

    @Test
    @DisplayName("Should return filtered Slice of ProductResponse based on multiple criteria")
    public void getProductsByRequests_validFilter_returnFilteredSlice(){

        for ( int i = 1; i <= 12; i++ ) {
            Product dumpProduct = Product.builder()
            .name("Product " + i)
            .basePrice(BigDecimal.valueOf(1000L * i))
            .sellPrice(BigDecimal.valueOf(1200L * i))
            .stockQuantity(BigDecimal.valueOf(10L * i))
            .stockMinimum(BigDecimal.valueOf(1L * i))
            .build();

            productRepository.save(dumpProduct);
        }

        Category category = Category.builder() 
        .name("Shoes")
        .build();

        Category saveAndFlush = categoryRepository.saveAndFlush(category);;

         Product dumpProduct = Product.builder()
            .name("Prodoteus XIJ ")
            .basePrice(BigDecimal.valueOf(5999L))
            .sellPrice(BigDecimal.valueOf(5999L))
            .stockQuantity(BigDecimal.valueOf(10L))
            .stockMinimum(BigDecimal.valueOf(0L))
            .category(saveAndFlush)
            .build();

        productRepository.save(dumpProduct);

        Product dumpProductInactive = Product.builder()
            .name("Product XIJ ")
            .basePrice(BigDecimal.valueOf(5999L))
            .sellPrice(BigDecimal.valueOf(5999L))
            .stockQuantity(BigDecimal.valueOf(10L))
            .stockMinimum(BigDecimal.valueOf(0L))
            .isActive(true)
            .build();

        productRepository.save(dumpProductInactive);

        ProductGetByFilter productGetByFilter = ProductGetByFilter.builder()
        .name("prod")
        .minPrice(BigDecimal.valueOf(2000L))
        .maxPrice(BigDecimal.valueOf(6000L))
        .page(0)
        .size(5)
        .sortBy("sellPrice")
        .sortDirection("ASC")
        .categoryId(saveAndFlush.getId())
        .build();

        SliceIndex<ProductResponse> productsByRequests = productService.getProductsByRequests(productGetByFilter);
        assertEquals(1, productsByRequests.getNumberOfElements());
        assertTrue(BigDecimal.valueOf(5999L).compareTo(productsByRequests.getContent().getFirst().sellPrice()) == 0);
        assertTrue(BigDecimal.valueOf(5999L).compareTo(productsByRequests.getContent().getLast().sellPrice()) == 0);
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when filtering by non-existent category ID")
    public void getProductsByRequests_invalidCategoryId_throwNotFoundEntityException(){
        when(categoryRepositoryMock.existsById(1L)).thenReturn(false);

        ProductGetByFilter productGetByFilter = ProductGetByFilter.builder()
        .name("prod")
        .minPrice(BigDecimal.valueOf(2000L))
        .maxPrice(BigDecimal.valueOf(6000L))
        .page(0)
        .size(5)
        .sortBy("sellPrice")
        .sortDirection("ASC")
        .categoryId(1L)
        .build();

        assertThrows(NotFoundEntityException.class, () -> productServiceMock.getProductsByRequests(productGetByFilter));
    }

    @Test
    @DisplayName("Should return realtime List of productOutOfStock")
    public void getOutOfStockProducts_foundData_returnListProjection(){
        when(productRepositoryMock.findAllOutOfStockProduct()).thenReturn(List.of(new ProductOutOfStock(1L, "Shoes", BigDecimal.ZERO , BigDecimal.ZERO)));

        List<ProductOutOfStock> outOfStockProducts = productServiceMock.getOutOfStockProducts();
        assertEquals(1, outOfStockProducts.size());
    }

    @Test
    @DisplayName("Should return cached response if data appear, test use different response between cache and db")
    public void getProductById_foundData_returnCache(){
      Cache cachedProductResponse = cacheManager.getCache("products");

      ProductResponse mockResponse = ProductResponse.builder()
          .id(1L)
          .name("Shoes")
          .build();

      cachedProductResponse.put(mockResponse.id(), mockResponse);

      ProductResponse product = productService.getProductById(mockResponse.id());
      assertEquals(product.name(), mockResponse.name());

    }

  @Test
  @DisplayName("Should return cached response if data appear")
  void getProducts_whenConditionMet_shouldReturnFromCache() {

    ProductGetByFilter request = ProductGetByFilter.builder()
        .sortBy("name")
        .sortDirection("ASC")
        .page(0)
        .size(10)
        .categoryId(null)
        .name(null)
        .minPrice(BigDecimal.ZERO)
        .maxPrice(BigDecimal.valueOf(50000000))
        .build();


    String cacheKey = "name_ASC_10";

    Cache cachedProducts = cacheManager.getCache("products:first-page");
    assertNotNull(cachedProducts);

    List<ProductResponse> mockResponseList = List.of(
        ProductResponse.builder()
            .id(1L)
            .name("Shoes")
            .build()
    );

    cachedProducts.put(cacheKey, new SliceImpl<>(mockResponseList));

    SliceIndex<ProductResponse> products = productService.getProductsByRequests(request);
    assertNotNull(products);
    assertEquals("Shoes", products.getContent().getFirst().name());
    assertEquals(1, products.getSize());


  }

  @Test
  @DisplayName("Should be hit the database because cache condition not meet")
  void getProducts_whenConditionNotMet_shouldHitRepository() {

    ProductGetByFilter request = ProductGetByFilter.builder()
        .sortBy("name")
        .sortDirection("ASC")
        .page(1)
        .size(10)
        .categoryId(null)
        .name(null)
        .minPrice(BigDecimal.ZERO)
        .maxPrice(BigDecimal.valueOf(50000000))
        .build();

    SliceIndex<ProductResponse> products = productService.getProductsByRequests(request);

    assertTrue(products.isEmpty());
  }




}
