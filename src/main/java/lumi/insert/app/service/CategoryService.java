package lumi.insert.app.service;

import lumi.insert.app.dto.request.CategoryGetRequest;
import org.springframework.data.domain.Slice;

import lumi.insert.app.dto.request.CategoryCreateRequest;
import lumi.insert.app.dto.request.CategoryUpdateRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.CategoryResponse;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    CategoryResponse updateCategoryName(CategoryUpdateRequest request);

    CategoryResponse activateCategory(Long id);

    CategoryResponse deactivateCategory(Long id);

    CategoryResponse getCategoryById(Long id);

    Slice<CategoryResponse> getCategories(CategoryGetRequest request);


}
