package lumi.insert.app.controller.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; 
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream; 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.core.repository.projection.ProductRefund;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.ProductGetByFilter;
import lumi.insert.app.dto.request.ProductGetNameRequest;
import lumi.insert.app.dto.response.ProductName;
import lumi.insert.app.dto.response.ProductResponse;
import lumi.insert.app.dto.response.ProductStockResponse;
import lumi.insert.app.dto.response.TransactionItemStatisticResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.utils.forTesting.ProductUtils;

public class ProductControllerGetTest extends BaseProductControllerTest{
    @Test
    @DisplayName("Should return 200 http status and response of product DTO")
    public void getProductAPI_validId_return200StatusAndResponseDTO() throws Exception{
        ProductResponse productResponse = ProductResponse.builder()
        .id(1L)
        .name("ProductMock")
        .basePrice(BigDecimal.valueOf(1000L))
        .build();

        when(productService.getProductById(1L)).thenReturn(productResponse);

        mockMvc.perform(
            get("/api/products/1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value(productResponse.name()))
        .andExpect(jsonPath("$.data.basePrice").value(BigDecimal.valueOf(1000L)));
    }

    @Test
    @DisplayName("Should return 404 http status when product ID not found")
    public void getProductAPI_idNotFound_return404StatusAndErrorResponse() throws Exception{
        when(productService.getProductById(1L)).thenThrow(new NotFoundEntityException("Product with ID " + 1L + " was not found"));

        mockMvc.perform(
            get("/api/products/1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors").value("Product with ID 1 was not found"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Should return 400 http status when product ID type is mismatched")
    public void getProductAPI_typeMismatch_return400StatusAndErrorResponse() throws Exception{
        mockMvc.perform(
            get("/api/products/were")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").value("id must be Long"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Should return 200 http status and list of product names")
    public void searchProductNamesAPI_validQuery_return200StatusAndListOfProductName() throws Exception{
        Slice<Product> mockSliceProduct = ProductUtils.getMockSliceProduct();

        Slice<ProductName> map = mockSliceProduct.map(product -> {
            ProductName productNameResponse = ProductName.builder()
                .id(product.getId())
                .name(product.getName())
                .build();
        
            return productNameResponse;
        });

        

        when(productService.searchProductNames(any(ProductGetNameRequest.class))).thenReturn(new SliceIndex<>(map));

        mockMvc.perform(
            get("/api/products/searchName?name=pro")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(12))
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("Should return 400 http status when search name is empty")
    public void getProductsNameAPI_emptyName_return400StatusAndErrorResponse() throws Exception{       
        mockMvc.perform(
            get("/api/products/searchName?name=")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").value("Name cannot be empty"));
    }

    @Test
    @DisplayName("Should return 200 http status and filtered list of products")
    public void getProductByFilterAPI_validFilter_return200StatusAndFilteredList() throws Exception{
        ArgumentCaptor<ProductGetByFilter> captor = ArgumentCaptor.forClass(ProductGetByFilter.class);

        Slice<Product> mockSliceProduct = ProductUtils.getMockSliceProduct();
        Slice<ProductResponse> map = mockSliceProduct.map(productMapper::createDtoResponseFromProduct);

        when(productService.getProductsByRequests(any())).thenReturn(new SliceIndex<>(map));

        mockMvc.perform(
            get("/api/products/filter?name=Pro&maxPrice=100000&sortBy=sellPrice")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(12));

        verify(productService).getProductsByRequests(captor.capture());
        ProductGetByFilter value = captor.getValue();

        assertEquals(0, value.getPage());
        assertEquals(10, value.getSize());
        assertEquals("Pro", value.getName());
        assertEquals(null, value.getCategoryId());
        assertEquals("sellPrice", value.getSortBy());
        assertTrue(BigDecimal.valueOf(100000).compareTo(value.getMaxPrice()) == 0);
    }

    @Test
    @DisplayName("Should return 404 http status when filtering with non-existent category")
    public void getProductByFilterAPI_categoryNotFound_return404StatusAndErrorResponse() throws Exception{
         when(productService.getProductsByRequests(any())).thenThrow(new NotFoundEntityException("Category with ID " + 1L + " was not found"));

         mockMvc.perform(
            get("/api/products/filter?categoryId=1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors").value("Category with ID 1 was not found"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Should return 400 http status when filter parameter type is invalid")
    public void getProductByFilterAPI_typeMismatch_return400StatusAndErrorResponse() throws Exception{
         mockMvc.perform(
            get("/api/products/filter?categoryId=true")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Should return error badRequest when filter parameter type violate ")
    public void getProductByFilterAPI_violateParameter_return400StatusAndErrorResponse() throws Exception{
         mockMvc.perform(
            get("/api/products/filter?minPrice=-5")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").value("minPrice minimal value is 0"))
        .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Should return error badRequest when filter parameter type violate ")
    public void getProductByFilterAPI_violateParameter2_return400StatusAndErrorResponse() throws Exception{
         mockMvc.perform(
            get("/api/products/filter?sortBy=sds")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").value("check documentation for sortBy specification"));
    }

    @Test
    @DisplayName("Should return 200 http status and product list with custom pagination")
    public void getAllProductsAPI_withParam_return200StatusAndPaginatedList() throws Exception{
        ArgumentCaptor<PaginationRequest> captor = ArgumentCaptor.forClass(PaginationRequest.class);

        Slice<Product> mockSliceProduct = ProductUtils.getMockSliceProduct();
        Slice<ProductResponse> map = mockSliceProduct.map(productMapper::createDtoResponseFromProduct);

        when(productService.getProducts(any())).thenReturn(map);

        mockMvc.perform(
            get("/api/products?size=12")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(12));

        verify(productService).getProducts(captor.capture());
        PaginationRequest value = captor.getValue();

        assertEquals(0, value.getPage());
        assertEquals(12, value.getSize());
    }

    @Test
    @DisplayName("Should return 200 http status and product list with default pagination")
    public void getAllProductsAPI_withoutParam_return200StatusAndDefaultPaginatedList() throws Exception{
        ArgumentCaptor<PaginationRequest> captor = ArgumentCaptor.forClass(PaginationRequest.class);

        Slice<Product> mockSliceProduct = ProductUtils.getMockSliceProduct();
        Slice<ProductResponse> map = mockSliceProduct.map(productMapper::createDtoResponseFromProduct);

        when(productService.getProducts(any())).thenReturn(map);

        mockMvc.perform(
            get("/api/products")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(12));

        verify(productService).getProducts(captor.capture());
        PaginationRequest value = captor.getValue();

        assertEquals(0, value.getPage());
        assertEquals(10, value.getSize());
    }

    @Test
    @DisplayName("Should return 200 http status and data of product's stock")
    public void getProductStockAPI_validRequest_return200StatusAndDtoStockResponse() throws Exception{
        ProductStockResponse productStockResponse = ProductStockResponse.builder()
        .id(1L)
        .stockQuantity(BigDecimal.valueOf(100L))
        .build();

        when(productService.getProductStock(anyLong())).thenReturn(productStockResponse);

         mockMvc.perform(
            get("/api/products/1/stocks")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isOk()) 
        .andExpect(jsonPath("$.data.stockQuantity").value(BigDecimal.valueOf(100L)))
        .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("Should return 404 http status when product was not found")
    public void getProductStockAPI_invalidIdNotFound_return404StatusAndErrorResponse() throws Exception{
        when(productService.getProductStock(anyLong())).thenThrow(new NotFoundEntityException("Product with ID " + 1 + " was not found"));

         mockMvc.perform(
            get("/api/products/1/stocks")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isNotFound()) 
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 http status when parameteer given was miss type")
    public void getProductStockAPI_missMatchTypeParam_return400StatusAndErrorResponse() throws Exception{
         mockMvc.perform(
            get("/api/products/1were/stocks")
            .accept(MediaType.APPLICATION_JSON_VALUE)
        )
        .andDo(print())
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 200 http status and pdf content when request valid")
    @WithMockUser(username = "admin", roles = "OWNER")
    public void getProductsStatisticsAPI_validRequest_return200StatusAndPdf() throws Exception{
        ByteArrayInputStream bais = new ByteArrayInputStream("pdfBinary".getBytes());

        TransactionItemStatisticResponse transactionItemStatisticResponse = TransactionItemStatisticResponse.builder().productRefunds(List.of(new ProductRefund("Shoes", BigDecimal.valueOf(1L)))).productSales(List.of()).build();

        when(transactionItemService.getTransactionItemStats(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(transactionItemStatisticResponse);
        when(productService.getOutOfStockProducts()).thenReturn(List.of());
        when(pdfService.exportProductsStatistic(any(TransactionItemStatisticResponse.class), anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(bais);

        MvcResult result = mockMvc.perform(
                get("/api/products/statistics/export?endDate=" + LocalDateTime.now())
                .accept(MediaType.APPLICATION_PDF)
            )
        .andDo(print()) 
        .andExpect(status().isOk()) 
        .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
        .andReturn();

        assertTrue(result.getResponse().getContentAsByteArray().length > 0);
        verify(transactionItemService, times(1)).getTransactionItemStats(argThat(arg -> arg.getDayOfMonth() == 1), argThat(arg -> arg.getDayOfMonth() == LocalDateTime.now().getDayOfMonth()));
        
        verify(pdfService, times(1)).exportProductsStatistic(argThat(arg -> arg.getProductRefunds().getFirst().productName().equals("Shoes") && arg.getProductSales().size() == 0), argThat(arg -> arg.size() == 0), any(), any());
    }

    @Test
    @DisplayName("Should return 400 http status when request type incompatible")
    public void getProductsStatisticsAPI_invalidType_return400Statu() throws Exception{

        mockMvc.perform(
                get("/api/products/statistics/export?type=docx") 
                .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON)
            )
        .andDo(print()) 
        .andExpect(status().isBadRequest()) 
        .andExpect(jsonPath("$.errors").value("check documentation for export type specification"));
 
    }
}
