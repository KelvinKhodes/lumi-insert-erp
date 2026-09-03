package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.TransactionPayment;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.CustomerRepository;
import lumi.insert.app.core.repository.TransactionPaymentRepository;
import lumi.insert.app.core.repository.TransactionRepository;
import lumi.insert.app.dto.request.TransactionPaymentGetByFilter;
import lumi.insert.app.utils.generator.InvoiceGenerator;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import({InvoiceGenerator.class, JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
@ActiveProfiles("test")
public class TransactionPaymentRepositoryTest  extends TestContainerTest {

    @Autowired
    TransactionRepository transactionRepository;
    
    @Autowired
    TransactionPaymentRepository transactionPaymentRepository; 

    @Autowired
    CustomerRepository customerRepository;

    @Autowired 
    InvoiceGenerator invoiceGenerator;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    Customer customer;

    @BeforeEach
    void setup(){
        EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("Test Username")
        .role(EmployeeRole.CASHIER)
        .ipAddress("t.e.s.t")
        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        customer = customerRepository.save(Customer.builder().id(UuidCreator.getTimeOrderedEpochFast()).name("Test").contact("test").shippingAddress("test").build());
    }
 

    @Test
    @DisplayName("Should add transaction_items entity to database when base field < invoiceId required is valid")
    public void createTransactionPayments_baseField_returnSavedEntity(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(customer)
        .customerName(customer.getName())
        .build();

       Transaction savedTransaction = transactionRepository.save(transaction);
 
        TransactionPayment transactionPayment = TransactionPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .transaction(savedTransaction)
        .totalPayment(BigDecimal.valueOf(1000L))
        .paymentFrom("Incart Global Lte - 2432131")
        .paymentTo("PT. Juke Ner - BCA 14123124")
        .build();

        TransactionPayment savedTransactionPayment= transactionPaymentRepository.saveAndFlush(transactionPayment);

        assertNotNull(savedTransactionPayment.getCreatedAt()); 
        assertEquals(savedTransaction.getId(), savedTransactionPayment.getTransaction().getId());
        assertEquals("Incart Global Lte - 2432131", savedTransactionPayment.getPaymentFrom());
    }

    @Test
    @DisplayName("Should return Slice of transaction_items entity")
    public void findAllByTransactionId_validRequest_returnPageableEntity(){
        String invoiceId = invoiceGenerator.generate();

            Transaction transaction = Transaction.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceId)
            .customer(customer)
            .customerName(customer.getName())
            .build();

            Transaction savedTransaction = transactionRepository.save(transaction);

            
        for (int i = 0; i < 3; i++) {
            TransactionPayment transactionPayment = TransactionPayment.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .transaction(savedTransaction)
                .totalPayment(BigDecimal.valueOf(1000L * i))
                .paymentFrom("Incart Global Lte - 2432131")
                .paymentTo("PT. Juke Ner - BCA 14123124")
                .build();

            transactionPaymentRepository.saveAndFlush(transactionPayment);
        }
        PageRequest pageable = PageRequest.of(0, 2, Sort.by("createdAt").ascending());
        Slice<TransactionPayment> searchedItem = transactionPaymentRepository.findAllByTransactionId(savedTransaction.getId(), pageable);

        assertEquals(2, searchedItem.getNumberOfElements());
        assertTrue(searchedItem.hasNext());
        assertTrue(BigDecimal.valueOf(1000L).compareTo(searchedItem.getContent().getLast().getTotalPayment()) == 0);
    }

    @Test
    @DisplayName("Should return Optional Empty entity when transaction and product id is not valid")
    public void findAllByTransactionId_invalidId_returnOptionalEmptyEntity(){
        Slice<TransactionPayment> searchedItem = transactionPaymentRepository.findAllByTransactionId(UUID.randomUUID(), PageRequest.of(0, 2, Sort.by("createdAt").ascending()));

        assertTrue(searchedItem.isEmpty());
    }

    @Test
    @DisplayName("Should return filtered transaction payment case 1: totalPayment between 1000 - 2000")
    public void findAllCriteria_totalPaymentBetween_returnTotalPayment1000To2000(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceId)
            .customer(customer)
            .customerName(customer.getName())
            .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        TransactionPayment transactionPaymentSuccess = TransactionPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(1500L))
        .transaction(savedTransaction)
        .build();

        TransactionPayment transactionPaymentFail = TransactionPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(2100L))
        .transaction(savedTransaction)
        .build();

        transactionPaymentRepository.saveAllAndFlush(List.of(transactionPaymentSuccess, transactionPaymentFail));

        TransactionPaymentGetByFilter request = TransactionPaymentGetByFilter.builder()
        .minTotalPayment(BigDecimal.valueOf(1000L))
        .maxTotalPayment(BigDecimal.valueOf(2000L))
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<TransactionPayment> specification = jpaSpecGenerator.transactionPaymentSpecification(request);

        Slice<TransactionPayment> transactionPayments = transactionPaymentRepository.findAll(specification, pageable);
        assertEquals(1, transactionPayments.getNumberOfElements());
        assertTrue(BigDecimal.valueOf(1500L).compareTo(transactionPayments.getContent().getFirst().getTotalPayment()) == 0);
    }

    @Test
    @DisplayName("Should return filtered transaction payment case 1: totalPayment between 1000 - 3000 and only from trx A")
    public void findAllCriteria_createdAtBetween_returnEmpty(){
        String invoiceIdA = invoiceGenerator.generate();

        Transaction transactionA = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceIdA)
            .customer(customer)
            .customerName(customer.getName())
            .build();

        Transaction transactionB = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
            .invoiceId(invoiceGenerator.generate())
            .customer(customer)
            .customerName(customer.getName())
            .build();

        List<Transaction> savedTransaction = transactionRepository.saveAllAndFlush(List.of(transactionA, transactionB));

        TransactionPayment transactionPaymentSuccess = TransactionPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(1500L))
        .transaction(savedTransaction.getFirst())
        .build();

        TransactionPayment transactionPaymentFail = TransactionPayment.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .paymentFrom("from")
        .paymentTo("to")
        .totalPayment(BigDecimal.valueOf(2100L))
        .transaction(savedTransaction.getLast())
        .build();

        transactionPaymentRepository.saveAllAndFlush(List.of(transactionPaymentSuccess, transactionPaymentFail));

        TransactionPaymentGetByFilter request = TransactionPaymentGetByFilter.builder()
        .minTotalPayment(BigDecimal.valueOf(1000L))
        .maxTotalPayment(BigDecimal.valueOf(3000L))
        .transactionId(savedTransaction.getLast().getId())
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<TransactionPayment> specification = jpaSpecGenerator.transactionPaymentSpecification(request);

        Slice<TransactionPayment> transactionPayments = transactionPaymentRepository.findAll(specification, pageable);
        assertEquals(1, transactionPayments.getNumberOfElements());
        assertTrue(BigDecimal.valueOf(2100L).compareTo(transactionPayments.getContent().getFirst().getTotalPayment()) == 0);
    }

}
