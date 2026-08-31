package lumi.insert.app.service;

import java.util.UUID;

import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import org.springframework.data.domain.Slice;

import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.TransactionPaymentCreateRequest;
import lumi.insert.app.dto.request.TransactionPaymentGetByFilter;
import lumi.insert.app.dto.response.TransactionPaymentResponse;

public interface TransactionPaymentService {

    TransactionPaymentResponse createTransactionPayment(UUID transactionId, TransactionPaymentCreateRequest request);

    Slice<TransactionPaymentResponse> getTransactionPaymentsByTransactionId(UUID transactionId, PaginationRequest request);

    TransactionPaymentResponse getTransactionPayment(UUID id);

    SliceIndex<TransactionPaymentResponse> getTransactionPaymentsByRequests(TransactionPaymentGetByFilter request);

    TransactionPaymentResponse refundTransactionPayment(UUID id, TransactionPaymentCreateRequest request);

}
