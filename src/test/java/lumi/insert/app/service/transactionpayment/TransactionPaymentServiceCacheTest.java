package lumi.insert.app.service.transactionpayment;


import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.core.repository.TransactionItemRepository;
import lumi.insert.app.core.repository.TransactionPaymentRepository;
import lumi.insert.app.dto.request.TransactionPaymentGetByFilter;
import lumi.insert.app.mapper.AllTransactionMapper;
import lumi.insert.app.mapper.AllTransactionMapperImpl;
import lumi.insert.app.service.TransactionItemService;
import lumi.insert.app.service.TransactionPaymentService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
public class TransactionPaymentServiceCacheTest extends TestContainerTest {

    @Autowired
    CacheManager cacheManager;
  
    @Autowired
    TransactionPaymentService transactionPaymentService;
  
    @MockitoBean
    TransactionPaymentRepository transactionPaymentRepository;

    @MockitoBean
    JpaSpecGenerator jpaSpecGenerator;
    @Spy
    AllTransactionMapper allTransactionMapper = new AllTransactionMapperImpl();

    @BeforeEach
    void setUp() {
      if (cacheManager.getCache("payments:first-page") != null) {
        cacheManager.getCache("payments:first-page").clear();
      };
    }
  
    @Test
    @DisplayName("should cache first-page when condition is met")
    void getStats_shouldCacheFirstPage() {
      TransactionPaymentGetByFilter request = TransactionPaymentGetByFilter.builder()
          .sortBy("createdAt")
          .sortDirection("DESC")
          .build();

      when(jpaSpecGenerator.pageable(any())).thenReturn(PageRequest.ofSize(10));
      when(jpaSpecGenerator.transactionPaymentSpecification(any())).thenReturn(mock(Specification.class));
        when(transactionPaymentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl(List.of()));

        transactionPaymentService.getTransactionPaymentsByRequests(request);
      transactionPaymentService.getTransactionPaymentsByRequests(request);

        verify(transactionPaymentRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));

        Cache firstPageCache = cacheManager.getCache("payments:first-page");
        assertNotNull(firstPageCache);
        assertNotNull(firstPageCache.get("createdAt_DESC_10"));
    }

  @Test
  @DisplayName("should cache first-page when condition is met")
  void getStats_shouldNotCacheFirstPage() {
    TransactionPaymentGetByFilter request = TransactionPaymentGetByFilter.builder()
        .sortBy("createdAt")
        .sortDirection("DESC")
        .transactionId(UuidCreator.getTimeOrderedEpochFast())
        .build();

    when(jpaSpecGenerator.pageable(any())).thenReturn(PageRequest.ofSize(10));
    when(jpaSpecGenerator.transactionPaymentSpecification(any())).thenReturn(mock(Specification.class));
    when(transactionPaymentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl(List.of()));

    transactionPaymentService.getTransactionPaymentsByRequests(request);
    transactionPaymentService.getTransactionPaymentsByRequests(request);

    verify(transactionPaymentRepository, times(2)).findAll(any(Specification.class), any(Pageable.class));

    Cache firstPageCache = cacheManager.getCache("payments:first-page");
    assertNotNull(firstPageCache);
    assertNull(firstPageCache.get("createdAt_DESC_10"));
  }
}

