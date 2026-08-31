package lumi.insert.app.controller.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.dto.request.CategoryGetRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;

import lumi.insert.app.core.entity.Category;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.CategoryResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.utils.forTesting.CategoryUtils;

public class CategoryControllerGetTest extends BaseCategoryControllerTest {

    @Test
    @DisplayName("Should return 200 http status when api called")
    public void getCategoriesAPI_return200() throws Exception{ 
        Category mockCategory = CategoryUtils.getMockCategory();

        Slice<Category> mockSliceCategory = new SliceImpl<Category>(List.of(mockCategory));
        Slice<CategoryResponse> resultMap = mockSliceCategory.map(categoryMapper::createDtoResponseFromCategory);

        when(categoryService.getCategories(any(CategoryGetRequest.class))).thenReturn(new SliceIndex<>(resultMap));

         mockMvc.perform(
            get("/api/categories")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(jsonPath("$.data.numberOfElements").value(1))
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("Should return 200 http status when request parameter is valid")
    public void getCategoryByIdAPI_validRequest_return200() throws Exception{ 
        Category mockCategory = CategoryUtils.getMockCategory();

        CategoryResponse dtoResponseFromCategory = categoryMapper.createDtoResponseFromCategory(mockCategory);
        when(categoryService.getCategoryById(any())).thenReturn(dtoResponseFromCategory);

        mockMvc.perform(
            get("/api/categories/1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(jsonPath("$.data.name").value("Category"))
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("Should return 400 http status when request parameter type is not valid")
    public void getCategoryByIdAPI_invalidIdRequest_return400() throws Exception{ 
        mockMvc.perform(
            get("/api/categories/were")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 404 http status when requested entity is not found")
    public void getCategoryByIdAPI_notFound_return404() throws Exception{ 
        when(categoryService.getCategoryById(any())).thenThrow(new NotFoundEntityException("Category with ID " + 1 + " was not found"));
        mockMvc.perform(
            get("/api/categories/1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isNotFound()) 
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

}
