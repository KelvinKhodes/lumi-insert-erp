package lumi.insert.app.service.transactionitem;


import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.core.repository.TransactionItemRepository;
import lumi.insert.app.dto.request.SupplierCreateRequest;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.request.SupplierUpdateRequest;
import lumi.insert.app.mapper.SupplierMapper;
import lumi.insert.app.mapper.SupplierMapperImpl;
import lumi.insert.app.service.SupplierService;
import lumi.insert.app.service.TransactionItemService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
public class TransactionItemServiceCacheTest extends TestContainerTest {

    @Autowired
    CacheManager cacheManager;
  
    @Autowired
    TransactionItemService transactionItemService;
  
    @MockitoBean
    TransactionItemRepository transactionItemRepository;

    @BeforeEach
    void setUp() {
      if (cacheManager.getCache("products-stats") != null) {
        cacheManager.getCache("products-stats").clear();
      };
    }
  
    @Test
    @DisplayName("should cache first-page when condition is met")
    void getStats_shouldCacheFirstPage() {
        LocalDateTime startDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        when(transactionItemRepository.getProductTopSales(any(), any())).thenReturn(List.of());
        when(transactionItemRepository.getProductTopRefund(any(), any())).thenReturn(List.of());

        transactionItemService.getTransactionItemStats(startDate, endDate);
        transactionItemService.getTransactionItemStats(startDate, endDate);

        verify(transactionItemRepository, times(1)).getProductTopSales(any(), any());
        verify(transactionItemRepository, times(1)).getProductTopRefund(any(), any());

        Cache firstPageCache = cacheManager.getCache("products-stats");
        assertNotNull(firstPageCache);
        assertNotNull(firstPageCache.get("30_30"));
    }

  @Test
  @DisplayName("should cache first-page when condition is met")
  void getStats_shouldNotCacheFirstPage() {
    LocalDateTime startDate = LocalDateTime.now().withHour(1).withMinute(0).withSecond(0);
    LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
    when(transactionItemRepository.getProductTopSales(any(), any())).thenReturn(List.of());
    when(transactionItemRepository.getProductTopRefund(any(), any())).thenReturn(List.of());

    transactionItemService.getTransactionItemStats(startDate, endDate);
    transactionItemService.getTransactionItemStats(startDate, endDate);

    verify(transactionItemRepository, times(2)).getProductTopSales(any(), any());
    verify(transactionItemRepository, times(2)).getProductTopRefund(any(), any());

    Cache firstPageCache = cacheManager.getCache("products-stats");
    assertNotNull(firstPageCache);
    assertNull(firstPageCache.get("30_30"));
  }
}

