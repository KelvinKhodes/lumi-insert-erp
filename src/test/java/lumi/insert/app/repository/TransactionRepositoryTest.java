package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime; 
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;  
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice; 
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.TransactionItem;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.entity.nondatabase.TransactionStatus;
import lumi.insert.app.core.repository.CustomerRepository;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.TransactionItemRepository;
import lumi.insert.app.core.repository.TransactionRepository;
import lumi.insert.app.dto.request.TransactionGetByFilter;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.utils.forTesting.ProductUtils;
import lumi.insert.app.utils.generator.InvoiceGenerator;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Slf4j
@Import({JpaSpecGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class})
@ActiveProfiles("test")
public class TransactionRepositoryTest  extends TestContainerTest {
    
    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    @Autowired
    TransactionItemRepository transactionItemRepository;

    @Autowired
    ProductRepository productRepository;
    
    @Autowired
    EntityManager entityManager;

    InvoiceGenerator invoiceGenerator = new InvoiceGenerator();

    Customer setupCustomer;

    @BeforeEach
    public void setup(){
        EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("Test Username")
        .role(EmployeeRole.CASHIER)
        .ipAddress("t.e.s.t")
        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        Customer customer1 = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("Customer 1")
        .email("TEST@mail.com")
        .contact("081234567890")
        .shippingAddress("Jl. xxx")
        .build();

        setupCustomer = customerRepository.saveAndFlush(customer1);
    }

    @Test
    @DisplayName("Should add transaction entity to database when base field < invoiceId required is valid")
    public void createTransaction_baseField_returnSavedEntity(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(setupCustomer)
        .customerName(setupCustomer.getName())
        .build();

      Transaction savedTransaction = transactionRepository.save(transaction);

      assertTrue(BigDecimal.ZERO.compareTo(savedTransaction.getGrandTotal()) == 0);
      assertEquals(TransactionStatus.PENDING, savedTransaction.getStatus());
      assertEquals(invoiceId, savedTransaction.getInvoiceId());
      assertNotNull(savedTransaction.getId());
    }

    @Test
    @DisplayName("Should thrown jpa data error when base field < invoiceId is null")
    public void createTransaction_nullableField_throwError(){
        Transaction transaction = Transaction.builder() 
        .id(UuidCreator.getTimeOrderedEpochFast())
        .customer(setupCustomer)
        .customerName(setupCustomer.getName())
        .build();
        
        assertNull(transaction.getInvoiceId());
        assertThrows(DataIntegrityViolationException.class, () -> transactionRepository.saveAndFlush(transaction));
    }

    @Test
    @DisplayName("Should return trx entity when invoiceId is valid")
    public void findByInvoiceId_validId_returnEntity(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(setupCustomer)
        .customerName(setupCustomer.getName())
        .build();

        transactionRepository.saveAndFlush(transaction);

        Transaction searchedTransaction = transactionRepository.findByInvoiceId(invoiceId).orElseThrow();

        assertEquals(invoiceId, searchedTransaction.getInvoiceId());
        assertNotNull(searchedTransaction.getCreatedAt());
        assertNotNull(searchedTransaction.getId());
    }

    @Test
    @DisplayName("Should return optional of empty when invoiceId is not valid")
    public void findByInvoiceId_invalidId_returnOptionalEmptyEntity(){

        assertThrows(NoSuchElementException.class, () -> transactionRepository.findByInvoiceId("S").orElseThrow());

    }

    @Test
    @DisplayName("Should return filtered transaction case 1: status.CANCELLED")
    public void findAllCriteria_statusCancelled_returnCancelledTransaction(){
        Set<Transaction> pendingTransactions = new HashSet<>();
        
        for (int i = 1; i < 3; i++) {
            if(i%2 == 0) {
                Transaction transaction = Transaction.builder()
                    .id(UuidCreator.getTimeOrderedEpochFast())
                    .invoiceId(invoiceGenerator.generate())
                    .status(TransactionStatus.CANCELLED)
                    .customer(setupCustomer)
                    .customerName(setupCustomer.getName())
                    .build();

                 pendingTransactions.add(transaction);
            } else {
                Transaction transaction = Transaction.builder()
                    .id(UuidCreator.getTimeOrderedEpochFast())
                    .invoiceId(invoiceGenerator.generate())
                    .customer(setupCustomer)
                    .customerName(setupCustomer.getName())
                    .build();

                 pendingTransactions.add(transaction);
            }
        }

        transactionRepository.saveAllAndFlush(pendingTransactions);

        TransactionGetByFilter request = TransactionGetByFilter.builder()
        .status(TransactionStatus.CANCELLED)
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<Transaction> specification = jpaSpecGenerator.transactionSpecification(request);
         
        Slice<Transaction> transactions = transactionRepository.findAll(specification, pageable);
        assertEquals(1, transactions.getNumberOfElements());
        assertEquals(TransactionStatus.CANCELLED, transactions.getContent().getFirst().getStatus());

    }

    @Test
    @DisplayName("Should return filtered transaction case 2: combine: status.PENDING && totalPaid between 100-200 && createdAt betwenn 2020-2021")
    public void findAllCriteria_combineFilter_returnCombinedFilter(){
        String matchInvoiceId = invoiceGenerator.generate();

        Transaction matchTransaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(matchInvoiceId)
        .totalPaid(BigDecimal.valueOf(150L))
        .customer(setupCustomer)
        .customerName(setupCustomer.getName())
        .build();

        matchTransaction.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        Transaction unmatchTransaction2 = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceGenerator.generate())
        .totalPaid(BigDecimal.valueOf(350L))
        .customer(setupCustomer)
        .customerName(setupCustomer.getName())
        .build();

        matchTransaction.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        Transaction unmatchTransaction3 = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceGenerator.generate())
        .status(TransactionStatus.CANCELLED)
        .totalPaid(BigDecimal.valueOf(150L))
        .customer(setupCustomer)
        .customerName(setupCustomer.getName())
        .build();

        matchTransaction.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        transactionRepository.saveAllAndFlush(List.of(matchTransaction, unmatchTransaction2, unmatchTransaction3));

        TransactionGetByFilter request = TransactionGetByFilter.builder() 
        .status(TransactionStatus.PENDING)
        .minTotalPaid(BigDecimal.valueOf(100L))
        .maxTotalPaid(BigDecimal.valueOf(200L))
        .minCreatedAt(LocalDateTime.of(2020, 1, 10, 10, 10))
        .maxCreatedAt(LocalDateTime.of(2027, 1, 10, 10, 10))
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<Transaction> specification = jpaSpecGenerator.transactionSpecification(request);

        Slice<Transaction> transactions = transactionRepository.findAll(specification, pageable);
        log.info("haleak, {}" + transactions.getContent().getFirst().getCreatedAt());
        assertEquals(1, transactions.getNumberOfElements());
        assertEquals(TransactionStatus.PENDING, transactions.getContent().getFirst().getStatus());
        assertEquals(matchInvoiceId, transactions.getContent().getFirst().getInvoiceId());
    }

    @Test
    @DisplayName("Should return filtered transaction case 3: get only customer 1")
    public void findAllCriteria_customerCase_returnCombinedFilter(){
        String matchInvoiceId = invoiceGenerator.generate();
 
        Customer customer2 = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("Customer 2")
        .email("TEST1@mail.com")
        .contact("0812314567890")
        .shippingAddress("Jl. xx1x")
        .build();

        Customer savedCustomer2 = customerRepository.saveAndFlush(customer2);;

        Transaction matchTransaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(matchInvoiceId)
        .totalPaid(BigDecimal.valueOf(150L))
        .customer(setupCustomer)
        .customerName(setupCustomer.getName())
        .build();

        matchTransaction.setCreatedAt(LocalDateTime.of(2020, 5, 10, 10, 10));

        Transaction unmatchTransaction2 = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceGenerator.generate())
        .totalPaid(BigDecimal.valueOf(350L))
        .customer(savedCustomer2)
        .customerName(setupCustomer.getName())
        .build();

        transactionRepository.saveAllAndFlush(List.of(matchTransaction, unmatchTransaction2));

        TransactionGetByFilter request = TransactionGetByFilter.builder()
        .customerId(setupCustomer.getId())
        .build();

        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<Transaction> specification = jpaSpecGenerator.transactionSpecification(request);

        Slice<Transaction> transactions = transactionRepository.findAll(specification, pageable); 
        assertEquals(1, transactions.getNumberOfElements());
        assertEquals(setupCustomer.getId(), transactions.getContent().getFirst().getCustomer().getId()); 
    }

    @Test
    @DisplayName("Should return transaction_items detail entity (with items > product) when transaction id is valid")
    public void findByIdDetail_validId_returnDetailEntity(){
        String invoiceId = invoiceGenerator.generate();

        Customer customer2 = Customer.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .name("Customer 2")
        .email("TEST1@mail.com")
        .contact("0812314567890")
        .shippingAddress("") 
        .build();

        Customer savedCustomer2 = customerRepository.saveAndFlush(customer2);

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(savedCustomer2)
        .customerName(savedCustomer2.getName())
        .build();

       Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.saveAndFlush(mockCategorizedProduct);

       TransactionItem transactionItem = TransactionItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .transaction(savedTransaction)
       .product(savedProduct)
       .productName(savedProduct.getName())
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

        transactionItemRepository.saveAndFlush(transactionItem);
        entityManager.clear();
        Transaction detailTransaction = transactionRepository.findByIdDetail(savedTransaction.getId()).orElseThrow(() -> new NotFoundEntityException(""));

        List<TransactionItem> transactionItems = detailTransaction.getTransactionItems();
        assertEquals(1, transactionItems.size());
        assertEquals(savedProduct.getId(), transactionItems.getFirst().getProduct().getId()); 
    }


}
