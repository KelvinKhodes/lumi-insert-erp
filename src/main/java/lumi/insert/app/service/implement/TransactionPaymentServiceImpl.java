package lumi.insert.app.service.implement;
 
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service; 
import org.springframework.web.multipart.MultipartFile;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.TransactionPayment;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EntityList;
import lumi.insert.app.core.entity.nondatabase.TransactionStatus;
import lumi.insert.app.core.entity.nondatabase.UploadStorageMessage;
import lumi.insert.app.core.repository.TransactionPaymentRepository;
import lumi.insert.app.core.repository.TransactionRepository;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.TransactionPaymentCreateRequest;
import lumi.insert.app.dto.request.TransactionPaymentGetByFilter;
import lumi.insert.app.dto.response.TransactionPaymentResponse;
import lumi.insert.app.exception.ForbiddenRequestException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.StorageActionException;
import lumi.insert.app.exception.TransactionValidationException;
import lumi.insert.app.mapper.AllTransactionMapper; 
import lumi.insert.app.service.TransactionPaymentService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

/**
 * Implementation of {@link TransactionPaymentService} managing financial settlements and proof of payment.
 * <p>
 * This service handles the final stages of the transaction lifecycle, ensuring that cash inflows 
 * and outflows (refunds) are accurately reflected in both transaction and customer balances.
 * </p>
 * * <h3>Key Responsibilities:</h3>
 * <ul>
 * <li><b>Payment Processing:</b> Records customer payments, reduces unpaid balances, and 
 * automatically completes transactions when debt reaches zero.</li>
 * <li><b>Refund Settlement:</b> Manages the payout of surplus balances (Unrefunded) to customers 
 * following item returns or order cancellations.</li>
 * <li><b>Asynchronous Evidence Storage:</b> Handles payment proof (receipts/images) by 
 * transferring {@link MultipartFile} to temporary storage and publishing 
 * {@link UploadStorageMessage} events for background processing.</li>
 * <li><b>Financial Integrity:</b> Prevents overpayment or over-refunding through strict 
 * {@link BigDecimal} validation.</li>
 * </ul>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class TransactionPaymentServiceImpl implements TransactionPaymentService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    TransactionPaymentRepository transactionPaymentRepository;

    @Autowired
    AllTransactionMapper allTransactionMapper;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    /**
     * Records a payment received from a customer for a specific transaction.
     * <p>
     * This method updates the debt status of both the {@link Transaction} and the {@link Customer}.
     * It also initiates an asynchronous file upload process for any payment attachments provided.
     * </p>
     *
     * @param transactionId the UUID of the transaction being paid.
     * @param request       the payment details including amount and proof files.
     * @return {@link TransactionPaymentResponse} confirming the recorded payment.
     * @throws TransactionValidationException if the payment amount exceeds the remaining debt.
     * @throws StorageActionException      if temporary file storage fails.
     */
    @Override
    @ActivityLogger(
        entityName = "transaction_payments",
        action = ActivityAction.TRANSACTION_PAYMENT_RECEIVED,
        actionMessage = "Transaction payment received from customer"
    )
    public TransactionPaymentResponse createTransactionPayment(UUID transactionId, TransactionPaymentCreateRequest request) {
        log.info("Creating transaction payment for transactionId={}, amount={}", transactionId, request.getTotalPayment());
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> {
                log.debug("Transaction payment creation failed, transaction not found id={}", transactionId);
                return new NotFoundEntityException("Transaction with ID " + transactionId + " was not found");
            });

        TransactionPayment transactionPayment = TransactionPayment.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .transaction(transaction)
            .paymentFrom(request.getPaymentFrom())
            .paymentTo(request.getPaymentTo())
            .totalPayment(request.getTotalPayment())
            .build();

        transaction.setTotalUnpaid(transaction.getTotalUnpaid().subtract(request.getTotalPayment()));
        transaction.setTotalPaid(transaction.getTotalPaid().add(request.getTotalPayment()));

        // Check if request payment exceeds the unpaid left
        if(transaction.getTotalUnpaid().compareTo(BigDecimal.ZERO) < 0) {
            log.debug("Payment exceeds unpaid amount for transactionId={}, totalPayment={}", transactionId, request.getTotalPayment());
            throw new TransactionValidationException("Payment exceeds the remaining transaction debts with ID " + transactionId + ", enter an exact amount to proceed");
        }

        Customer customer = transaction.getCustomer();
        customer.setTotalUnpaid(customer.getTotalUnpaid().subtract(request.getTotalPayment()));
        customer.setTotalPaid(customer.getTotalPaid().add(request.getTotalPayment()));
        // Upcoming: integrate with email notification
        if(transaction.getTotalUnpaid().compareTo(BigDecimal.ZERO) == 0) transaction.setStatus(TransactionStatus.COMPLETE);
        TransactionPayment savedTransactionPayment = transactionPaymentRepository.save(transactionPayment);
        TransactionPaymentResponse transactionPaymentResponseDto = allTransactionMapper.createTransactionPaymentResponseDto(savedTransactionPayment);

        // Upload payment proof/others to storage via message producer
        MultipartFile[] files = request.getFiles();
        List<Path> paths = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                // Save to temporary
                Path tempFile = Files.createTempFile("paymentOf" + transaction.getId() + "-", "_upload");
                file.transferTo(tempFile);
                paths.add(tempFile);
                
            }    
        } catch (IOException e) {
            log.error("Store file failed for transactionId={}, messages={}", transactionId, e.getMessage());
            throw new StorageActionException("Server couldn't complete the request due to internal problem, try again or contact developer");
        }
 
        paths.forEach(path -> {
            // Publish event to EventListener < listen and pass msg producer service
            eventPublisher.publishEvent(
                new UploadStorageMessage(
                    EntityList.TRANSACTION_PAYMENT,
                    transactionPayment.getId(), 
                    path.toAbsolutePath().toString(),
                    ((EmployeeLogin) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                )
            );
        });                    
  
        log.info("Transaction payment created paymentId={}, transactionId={}", savedTransactionPayment.getId(), transactionId);
        return transactionPaymentResponseDto;
    }

    /**
     * Retrieves a paginated list of payment history for a specific transaction.
     *
     * @param transactionId the transaction identifier.
     * @param request       pagination parameters (page, size).
     * @return a {@link Slice} of transaction payment records.
     */
    @Override
    public Slice<TransactionPaymentResponse> getTransactionPaymentsByTransactionId(UUID transactionId, PaginationRequest request) {
        log.info("Retrieving transaction payments for transactionId={}, page={}, size={}", transactionId, request.getPage(), request.getSize());
        Sort sort = Sort.by("createdAt").ascending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize()).withSort(sort);

        Slice<TransactionPayment> transactionPayments = transactionPaymentRepository.findAllByTransactionId(transactionId, pageable);
        Slice<TransactionPaymentResponse> result = transactionPayments.map(allTransactionMapper::createTransactionPaymentResponseDto);

        return result;
    }

    /**
     * Retrieves the details of a specific payment record by its ID.
     *
     * @param id the unique identifier of the payment.
     * @return the {@link TransactionPaymentResponse}.
     */
    @Override
    public TransactionPaymentResponse getTransactionPayment(UUID id) {
        log.info("Retrieving transaction payment id={}", id);
        TransactionPayment transactionPayment = transactionPaymentRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Transaction payment not found id={}", id);
                return new NotFoundEntityException("Transaction Payment with ID " + id + " was not found");
            });
        
        TransactionPaymentResponse transactionPaymentResponseDto = allTransactionMapper.createTransactionPaymentResponseDto(transactionPayment);
        return transactionPaymentResponseDto;
    }

    /**
     * Searches for payment records across the system using dynamic criteria.
     *
     * @param request filter criteria (e.g., date range, payment source).
     * @return a {@link Slice} of matching transaction payments.
     */
    @Override
    @Cacheable(
        value = "payments:first-page",
        key = "#request.sortBy + '_' + #request.sortDirection + '_' + #request.getSize",
        condition = "#request.isForDashboard()"
    )
    public Slice<TransactionPaymentResponse> getTransactionPaymentsByRequests(TransactionPaymentGetByFilter request) {
        log.info("Searching transaction payments with filters page={}, size={}", request.getPage(), request.getSize());
        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<TransactionPayment> specification = jpaSpecGenerator.transactionPaymentSpecification(request);

        Slice<TransactionPayment> transactionPayments = transactionPaymentRepository.findAll(specification, pageable);
        Slice<TransactionPaymentResponse> result = transactionPayments.map(allTransactionMapper::createTransactionPaymentResponseDto);

        return result;
    }

    /**
     * Records a refund payment sent back to the customer.
     * <p>
     * This is used to settle {@code totalUnrefunded} amounts caused by returns. 
     * The transaction status will move to {@code COMPLETE} once all surplus 
     * balances have been settled back to the customer.
     * </p>
     *
     * @param transactionId the UUID of the transaction associated with the refund.
     * @param request       refund details and proof of payout.
     * @return the recorded refund payment details.
     * @throws ForbiddenRequestException      if the transaction is still in PENDING or already COMPLETE.
     * @throws TransactionValidationException if the refund amount exceeds the unrefunded balance.
     */
    @Override
    @ActivityLogger(
        entityName = "transaction_payments",
        action = ActivityAction.TRANSACTION_REFUND_SETTLED,
        actionMessage = "Transaction refund settled to customer"
    )
    public TransactionPaymentResponse refundTransactionPayment(UUID transactionId, TransactionPaymentCreateRequest request) {
        log.info("Creating refund transaction payment for transactionId={}, amount={}", transactionId, request.getTotalPayment());
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> {
                log.debug("Refund failed, transaction not found id={}", transactionId);
                return new NotFoundEntityException("Transaction with ID " + transactionId + " was not found");
            });
 
        if (transaction.getStatus() == TransactionStatus.PENDING || transaction.getStatus() == TransactionStatus.COMPLETE ) {
            log.debug("Refund payment attempted on invalid transaction status transactionId={}, status={}", transactionId, transaction.getStatus());
            throw new ForbiddenRequestException("Refund payment only to Transaction with status PROCESS(onGoing) or CANCELLED, check carefully");
        }

        BigDecimal totalUnrefunded = transaction.getTotalUnrefunded();

        // Check if request payment exceeds the unpaid left
        if(request.getTotalPayment().compareTo(totalUnrefunded) > 0) {
            log.debug("Refund payment exceeds unrefunded amount transactionId={}, requestAmount={}, remaining={}", transactionId, request.getTotalPayment(), totalUnrefunded);
            throw new TransactionValidationException("Payment refund exceeds the remaining transaction unrefunded debt with ID " + transaction.getId() + ", enter an exact amount to proceed");
        }

        TransactionPayment refundTransactionPayment = TransactionPayment.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .transaction(transaction)
            .paymentFrom(request.getPaymentFrom())
            .paymentTo(request.getPaymentTo())
            .totalPayment(request.getTotalPayment())
            .isForRefund(true)
            .build();

        Customer customer = transaction.getCustomer();
        customer.setTotalRefunded(customer.getTotalRefunded().add(request.getTotalPayment()));
        customer.setTotalUnrefunded(customer.getTotalUnrefunded().subtract(request.getTotalPayment()));

        transaction.setTotalRefunded(transaction.getTotalRefunded().add(request.getTotalPayment()));
        transaction.setTotalUnrefunded(transaction.getTotalUnrefunded().subtract(request.getTotalPayment()));
        
        // Upcoming: integrate with email notification
        if(transaction.getTotalUnrefunded().compareTo(BigDecimal.ZERO) == 0) transaction.setStatus(TransactionStatus.COMPLETE);
        TransactionPayment savedTransactionPayment = transactionPaymentRepository.save(refundTransactionPayment);
        
        TransactionPaymentResponse transactionPaymentResponseDto = allTransactionMapper.createTransactionPaymentResponseDto(savedTransactionPayment);
        log.info("Refund payment created paymentId={}, transactionId={}", savedTransactionPayment.getId(), transactionId);
        
        // Upload payment proof/others to storage via message producer
        MultipartFile[] files = request.getFiles();
        List<Path> paths = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                // Save to temporary
                Path tempFile = Files.createTempFile("refPaymentOf" + transaction.getId() + "-", "_upload");
                file.transferTo(tempFile);
                paths.add(tempFile);
                
            }    
        } catch (IOException e) {
            log.error("Store file failed for transactionId={}, messages={}", transactionId, e.getMessage());
            throw new StorageActionException("Server couldn't complete the request due to internal problem, try again or contact developer");
        }
 
        paths.forEach(path -> {
            // Publish event to EventListener < listen and pass msg producer service
            eventPublisher.publishEvent(
                new UploadStorageMessage(
                    EntityList.TRANSACTION_PAYMENT,
                    savedTransactionPayment.getId(), 
                    path.toAbsolutePath().toString(),
                    ((EmployeeLogin) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                )
            );
        });    

        return transactionPaymentResponseDto;
    }
    
}
