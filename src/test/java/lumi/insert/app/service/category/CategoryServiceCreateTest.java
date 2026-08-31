package lumi.insert.app.service.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lumi.insert.app.dto.request.ProductCreateRequest;
import lumi.insert.app.dto.response.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lumi.insert.app.core.entity.Category;
import lumi.insert.app.dto.request.CategoryCreateRequest;
import lumi.insert.app.dto.response.CategoryResponse;
import lumi.insert.app.exception.DuplicateEntityException;
import org.springframework.cache.Cache;

public class CategoryServiceCreateTest extends BaseCategoryServiceTest{

    @Test
    @DisplayName("Should return created CategoryResponse DTO when request is valid")
    public void createCategory_validRequest_returnResponseDTO(){
        when(categoryRepositoryMock.existsByName("Electronics")).thenReturn(false);

        CategoryCreateRequest categoryCreateRequest = CategoryCreateRequest.builder()
        .name("Electronics")
        .build();

        Category mockCategoryQuery = Category.builder()
        .name("Electronics")
        .build();

        Category mockCategoryResult = Category.builder()
        .id(1L)
        .name("Electronics")
        .build();

        mockCategoryResult.setCreatedAt(LocalDateTime.now());
        mockCategoryResult.setUpdatedAt(LocalDateTime.now());

        when(categoryRepositoryMock.save(mockCategoryQuery)).thenReturn(mockCategoryResult);

        CategoryResponse categoryResponse = new CategoryResponse(mockCategoryResult.getId(), mockCategoryResult.getName(), null, null,LocalDateTime.now(), LocalDateTime.now());
        when(categoryMapper.createDtoResponseFromCategory(mockCategoryResult)).thenReturn(categoryResponse);

        CategoryResponse mockCategoryResponse = categoryServiceMock.createCategory(categoryCreateRequest);
        assertNotNull(mockCategoryResponse);
        assertEquals(1L, mockCategoryResponse.id());
        assertEquals("Electronics", mockCategoryResponse.name());
        assertNotNull(mockCategoryResponse.createdAt());
        assertNotNull(mockCategoryResponse.updatedAt());
    }

    @Test
    @DisplayName("Should throw DuplicateEntityException when category name already exists")
    public void createCategory_existingName_throwDuplicateEntityException(){
        when(categoryRepositoryMock.existsByName("Shoes")).thenReturn(true);

        CategoryCreateRequest categoryCreateRequest = CategoryCreateRequest.builder()
        .name("Shoes")
        .build();

        assertThrows(DuplicateEntityException.class, () -> categoryServiceMock.createCategory(categoryCreateRequest));
    }

    @Test
    @DisplayName("Should evict categories cache when create a category")
    public void createCategory_evictCache(){
        Cache cachedCategories = cacheManager.getCache("categories:first-page");

        cachedCategories.put("random", "random");

        CategoryCreateRequest request = CategoryCreateRequest.builder()
            .name("Smartphone")
            .build();

        categoryService.createCategory(request);
        assertNull(cachedCategories.get("random"));
    }
}
