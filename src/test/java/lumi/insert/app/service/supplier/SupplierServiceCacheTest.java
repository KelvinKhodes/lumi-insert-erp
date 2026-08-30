package lumi.insert.app.service.supplier;


import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.dto.request.SupplierCreateRequest;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.request.SupplierUpdateRequest;
import lumi.insert.app.mapper.SupplierMapper;
import lumi.insert.app.mapper.SupplierMapperImpl;
import lumi.insert.app.service.SupplierService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
public class SupplierServiceCacheTest extends TestContainerTest {

    @Autowired
    CacheManager cacheManager;
  
    @Autowired
    SupplierService supplierService;
  
    @MockitoBean
    SupplierRepository supplierRepository;
  
    @MockitoBean
    JpaSpecGenerator jpaSpecGenerator;
  
    @Spy
    SupplierMapper SupplierMapper = new SupplierMapperImpl();
  
    @BeforeEach
    void setUp() {
      if (cacheManager.getCache("suppliers:first-page") != null) {
        cacheManager.getCache("suppliers:first-page").clear();
      };
    }
  
    @Test
    @DisplayName("should cache first-page when condition is met")
    void getSuppliers_shouldCacheFirstPage() {
      SupplierGetByFilter request = SupplierGetByFilter.builder()
          .sortBy("createdAt")
          .sortDirection("DESC")
          .size(10)
          .build();

      when(jpaSpecGenerator.pageable(any())).thenReturn(Pageable.ofSize(10));
      when(jpaSpecGenerator.supplierSpecification(any())).thenReturn(mock(Specification.class));
      when(supplierRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl(List.of()));
  
      supplierService.getSuppliers(request);
      supplierService.getSuppliers(request);
  
      verify(supplierRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  
      Cache firstPageCache = cacheManager.getCache("suppliers:first-page");
      assertNotNull(firstPageCache);
      assertNotNull(firstPageCache.get("createdAt_DESC_10"));
    }
  
    @Test
    @DisplayName("should not cache first-page when condition is not met")
    void getSuppliers_shouldNotCacheFirstPage() {
      SupplierGetByFilter request = SupplierGetByFilter.builder()
          .sortBy("createdAt")
          .sortDirection("DESC")
          .size(13)
          .build();

      when(jpaSpecGenerator.pageable(any())).thenReturn(Pageable.ofSize(10));
      when(jpaSpecGenerator.supplierSpecification(any())).thenReturn(mock(Specification.class));
      when(supplierRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl(List.of()));
  
      supplierService.getSuppliers(request);
      supplierService.getSuppliers(request);
  
      verify(supplierRepository, times(2)).findAll(any(Specification.class), any(Pageable.class));
  
      Cache firstPageCache = cacheManager.getCache("suppliers:first-page");
      assertNotNull(firstPageCache);
      assertNull(firstPageCache.get("createdAt_DESC_13"));
    }
  
    @Test
    @DisplayName("Should evict first page cache when create a supplier")
    public void createSupplier_evictCache(){
        Cache cachedSuppliers = cacheManager.getCache("suppliers:first-page");
    
        cachedSuppliers.put("random", "random");
    
        SupplierCreateRequest request = SupplierCreateRequest
            .builder()
            .name("Test")
            .email("Test@mail.com")
            .contact("0000000")
            .build();
    
        when(supplierRepository.existsByName(any())).thenReturn(false);
        when(supplierRepository.save(any())).thenReturn(new Supplier());
    
        supplierService.createSupplier(request);
        assertNull(cachedSuppliers.get("random"));
    }

  @Test
  @DisplayName("Should evict first page cache when update a Supplier")
  public void updateSupplier_evictCache(){
    Cache cachedSuppliers = cacheManager.getCache("suppliers:first-page");

    cachedSuppliers.put("random", "random");

    SupplierUpdateRequest request = SupplierUpdateRequest
        .builder()
        .email("Test@mail.com")
        .build();

    when(supplierRepository.findById(any())).thenReturn(Optional.of(new Supplier()));

    supplierService.updateSupplier(UuidCreator.getTimeOrderedEpochFast(), request);
    assertNull(cachedSuppliers.get("random"));
  }
 
}

