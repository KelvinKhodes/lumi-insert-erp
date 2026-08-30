package lumi.insert.app.service.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import lumi.insert.app.dto.request.CategoryCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lumi.insert.app.core.entity.Category;
import lumi.insert.app.dto.request.CategoryUpdateRequest;
import lumi.insert.app.dto.response.CategoryResponse;
import lumi.insert.app.exception.BoilerplateRequestException;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.NotFoundEntityException;
import org.springframework.cache.Cache;

public class CategoryServiceEditTest extends BaseCategoryServiceTest {
    
    @Test
    @DisplayName("Should return updated CategoryResponse DTO when category name update is successful")
    public void updateCategoryName_validRequest_returnUpdatedResponseDTO(){
        Category mockCategory = Category.builder()
        .id(1L)
        .name("unChangedName")
        .build();

        Category mockCategoryEdit = Category.builder()
        .id(1L)
        .name("changedName")
        .build();

        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.of(mockCategory));

        when(categoryRepositoryMock.save(any(Category.class))).thenReturn(mockCategoryEdit);

        CategoryUpdateRequest categoryEditRequest = CategoryUpdateRequest.builder()
        .id(1L)
        .name("changedName")
        .build();
        
        CategoryResponse categoryResponse = new CategoryResponse(mockCategoryEdit.getId(), mockCategoryEdit.getName(),null, null, null, null);

        when(categoryMapper.createDtoResponseFromCategory(mockCategoryEdit)).thenReturn(categoryResponse);

        CategoryResponse editCategoryName = categoryServiceMock.updateCategoryName(categoryEditRequest);

        assertEquals("changedName", editCategoryName.name());
        assertEquals(1L, editCategoryName.id());
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when updating non-existent category ID")
    public void updateCategoryName_idNotFound_throwNotFoundEntityException(){
        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.empty());

        CategoryUpdateRequest categoryEditRequest = CategoryUpdateRequest.builder()
        .id(1L)
        .name("changedName")
        .build();

        assertThrows(NotFoundEntityException.class, () -> categoryServiceMock.updateCategoryName(categoryEditRequest));
    }

    @Test
    @DisplayName("Should throw DuplicateEntityException when request name is exists")
    public void updateCategoryName_duplicateName_throwDuplicateEntityException(){
        when(categoryRepositoryMock.existsByName("changedName")).thenReturn(true);

        CategoryUpdateRequest categoryEditRequest = CategoryUpdateRequest.builder()
        .id(1L)
        .name("changedName")
        .build();

        assertThrows(DuplicateEntityException.class, () -> categoryServiceMock.updateCategoryName(categoryEditRequest));
    }

    @Test
    @DisplayName("Should return CategoryResponse with active status when activation is successful")
    public void activateCategory_validId_returnActiveResponseDTO(){
        Category mockCategory = Category.builder()
        .name("mockCategory")
        .isActive(false)
        .build();

        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.of(mockCategory));

        when(categoryRepositoryMock.save(any(Category.class)))
        .thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = new CategoryResponse(
        1L, "mockCategory", null, null, null, null);

        when(categoryMapper.createDtoResponseFromCategory(any(Category.class)))
        .thenReturn(response);

        CategoryResponse editCategoryName = categoryServiceMock.activateCategory(1L);

        assertEquals("mockCategory", editCategoryName.name());
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when activating non-existent category ID")
    public void activateCategory_idNotFound_throwNotFoundEntityException(){
        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> categoryServiceMock.activateCategory(1L));
    }

    @Test
    @DisplayName("Should throw BoilerplateRequestException when trying to activate an already active category")
    public void activateCategory_alreadyActive_throwBoilerplateRequestException(){
        Category mockCategory = Category.builder()
        .name("mockCategory")
        .isActive(true)
        .build();

        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.of(mockCategory));

        assertThrows(BoilerplateRequestException.class, () -> categoryServiceMock.activateCategory(1L));
    }

    @Test
    @DisplayName("Should return CategoryResponse with inactive status when deactivation is successful")
    public void deactivateCategory_validId_returnInactiveResponseDTO(){
        Category mockCategory = Category.builder()
        .name("mockCategory")
        .build();

        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.of(mockCategory));

        when(categoryRepositoryMock.save(any(Category.class)))
        .thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = new CategoryResponse(
        1L, "mockCategory", null, null, null, null);

        when(categoryMapper.createDtoResponseFromCategory(any(Category.class)))
        .thenReturn(response);

        CategoryResponse editCategoryName = categoryServiceMock.deactivateCategory(1L);

        assertEquals("mockCategory", editCategoryName.name());
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when deactivating non-existent category ID")
    public void deactivateCategory_idNotFound_throwNotFoundEntityException(){
        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> categoryServiceMock.deactivateCategory(1L));
    }

    @Test
    @DisplayName("Should throw BoilerplateRequestException when trying to deactivate an already inactive category")
    public void deactivateCategory_alreadyInactive_throwBoilerplateRequestException(){
        Category mockCategory = Category.builder()
        .name("mockCategory")
        .isActive(false)
        .build();

        when(categoryRepositoryMock.findById(1L)).thenReturn(Optional.of(mockCategory));

        assertThrows(BoilerplateRequestException.class, () -> categoryServiceMock.deactivateCategory(1L));
    }

    @Test
    @DisplayName("Should evict categories cache when update a category")
    public void updateCategory_evictCache(){
        Cache cachedCategories = cacheManager.getCache("categories:first-page");

        cachedCategories.put("random", "random");

        Category smartpho = categoryRepository.save(Category.builder().name("Smartpho").build());

        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
            .id(smartpho.getId())
            .name("Smartphone")
            .build();

        categoryService.updateCategoryName(request);
        assertNull(cachedCategories.get("random"));
    }

    @Test
    @DisplayName("Should evict categories cache when update a category")
    public void deactivateCategory_evictCache(){
        Cache cachedCategories = cacheManager.getCache("categories:first-page");

        cachedCategories.put("random", "random");

        Category smartpho = categoryRepository.save(Category.builder().name("Smartpho").build());

        categoryService.deactivateCategory(smartpho.getId());
        assertNull(cachedCategories.get("random"));
    }

    @Test
    @DisplayName("Should evict categories cache when update a category")
    public void activateCategory_evictCache(){
        Cache cachedCategories = cacheManager.getCache("categories:first-page");

        cachedCategories.put("random", "random");

        Category smartpho = categoryRepository.save(Category.builder().isActive(false).name("Smartpho").build());

        categoryService.activateCategory(smartpho.getId());
        assertNull(cachedCategories.get("random"));
    }

    @Test
    @DisplayName("Shouldn't evict categories cache when throw exception")
    public void activateCategory_throw_wontEvictCache(){
        Cache cachedCategories = cacheManager.getCache("categories:first-page");

        cachedCategories.put("random", "random");

        Category smartpho = categoryRepository.save(Category.builder().isActive(false).name("Smartpho").build());

        assertThrows(BoilerplateRequestException.class, () -> categoryService.deactivateCategory(smartpho.getId()));
        assertNotNull(cachedCategories.get("random"));
    }

}
