package lumi.insert.app.service.transactionpayment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;

import lumi.insert.app.core.entity.TransactionPayment;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.TransactionPaymentGetByFilter; 
import lumi.insert.app.dto.response.TransactionPaymentResponse;
import lumi.insert.app.exception.NotFoundEntityException;

public class TransactionPaymentServiceGetTest extends BaseTransactionPaymentServiceTest{

    @Test
    @DisplayName("Should return TransactionPaymentResponce when transaction payment found")
    public void getTransactionPayment_validId_returnTransactionPaymentDTO(){
        setupTransactionPayment.setTotalPayment(BigDecimal.valueOf(1000L));
        when(transactionPaymentRepositoryMock.findById(setupTransactionPayment.getId())).thenReturn(Optional.of(setupTransactionPayment));

        TransactionPaymentResponse result = transactionPaymentServiceMock.getTransactionPayment(setupTransactionPayment.getId());
        assertEquals(setupTransactionPayment.getId(), result.id());
        assertTrue(BigDecimal.valueOf(1000L).compareTo(result.totalPayment()) == 0);
    }

    @Test
    @DisplayName("Should thrown not found error when transaction payment not found")
    public void getTransactionPayment_invalidId_throwNotFoundError(){ 
        when(transactionPaymentRepositoryMock.findById(any())).thenReturn(Optional.empty()); 

        assertThrows(NotFoundEntityException.class, () -> transactionPaymentServiceMock.getTransactionPayment(null));
    }

    @Test
    @DisplayName("Should return Slice of TransactionPaymentResponce when transaction payment with containing trx id found")
    public void getTransactionPaymentByTrxId_validTrxId_returnSliceTransactionPaymentDTO(){
        setupTransactionPayment.setTotalPayment(BigDecimal.valueOf(1000L));
        Slice<TransactionPayment> slices = new SliceImpl<>(List.of(setupTransactionPayment));
        when(transactionPaymentRepositoryMock.findAllByTransactionId(eq(setupTransactionPayment.getId()), any())).thenReturn(slices);

        Slice<TransactionPaymentResponse> result = transactionPaymentServiceMock.getTransactionPaymentsByTransactionId(setupTransactionPayment.getId(), PaginationRequest.builder().build());
        assertEquals(setupTransactionPayment.getId(), result.getContent().getFirst().id());
        assertTrue(BigDecimal.valueOf(1000L).compareTo(result.getContent().getFirst().totalPayment()) == 0);
        assertEquals(1, result.getNumberOfElements());
    }

    @Test
    @DisplayName("Should return Slice of TransactionPaymentResponce when transaction payment with containing trx id found")
    public void getTransactionPaymentsByRequest_validTrxId_returnSliceTransactionPaymentDTO(){
        setupTransactionPayment.setTotalPayment(BigDecimal.valueOf(1000L));
        Page<TransactionPayment> slices = new PageImpl<TransactionPayment>((List.of(setupTransactionPayment)));
        when(jpaSpecGenerator.pageable(any())).thenReturn(PageRequest.of(0, 10));
        when(jpaSpecGenerator.transactionPaymentSpecification(any())).thenReturn(Specification.anyOf(List.of()));
        when(transactionPaymentRepositoryMock.findAll(ArgumentMatchers.<Specification<TransactionPayment>>any(), any(Pageable.class))).thenReturn(slices);

        SliceIndex<TransactionPaymentResponse> result = transactionPaymentServiceMock.getTransactionPaymentsByRequests(TransactionPaymentGetByFilter.builder().build());
        assertEquals(setupTransactionPayment.getId(), result.getContent().getFirst().id());
        assertTrue(BigDecimal.valueOf(1000L).compareTo(result.getContent().getFirst().totalPayment()) == 0);
        assertEquals(1, result.getNumberOfElements());
    }
}
