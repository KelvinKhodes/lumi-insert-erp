package lumi.insert.app.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.config.security.AuditorAwareImpl;
import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.TransactionItem;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.CustomerRepository;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.TransactionItemRepository;
import lumi.insert.app.core.repository.TransactionRepository;
import lumi.insert.app.core.repository.projection.ProductRefund;
import lumi.insert.app.core.repository.projection.ProductSale;
import lumi.insert.app.utils.forTesting.ProductUtils;
import lumi.insert.app.utils.generator.InvoiceGenerator;

@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import({InvoiceGenerator.class, AuditorAwareImpl.class, SimpleCacheManager.class })
@ActiveProfiles("test")
public class TransactionItemRepositoryTest  extends TestContainerTest {

    @Autowired
    TransactionRepository transactionRepository;
    
    @Autowired
    TransactionItemRepository transactionItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired 
    InvoiceGenerator invoiceGenerator;

    @Autowired
    CustomerRepository customerRepository;
 

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
        
        customer = customerRepository.save(Customer.builder().id(UuidCreator.getTimeOrderedEpochFast()).name("TESTES").contact("TESTE123").shippingAddress("SHIPTEST").build());
    }

    @Test
    @DisplayName("Should add transaction_items entity to database when base field < invoiceId required is valid")
    public void createTransactionItems_baseField_returnSavedEntity(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(customer)
        .customerName(customer.getName())
        .build();

       Transaction savedTransaction = transactionRepository.save(transaction);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.save(mockCategorizedProduct);

       TransactionItem transactionItem = TransactionItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .transaction(savedTransaction)
       .product(savedProduct)
       .productName(savedProduct.getName())
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

        TransactionItem saveAndFlushTrxItems = transactionItemRepository.saveAndFlush(transactionItem);

        assertNotNull(saveAndFlushTrxItems.getCreatedAt());
        assertEquals(savedProduct.getId(), saveAndFlushTrxItems.getProduct().getId());
        assertEquals(savedTransaction.getId(), saveAndFlushTrxItems.getTransaction().getId());
    }

    @Test
    @DisplayName("Should return transaction_items entity when transaction and product id is valid")
    public void findByTransactionIdAndProductId_validId_returnEntity(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(customer)
        .customerName(customer.getName())
        .build();

       Transaction savedTransaction = transactionRepository.save(transaction);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.save(mockCategorizedProduct);

       TransactionItem transactionItem = TransactionItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .transaction(savedTransaction)
       .product(savedProduct)
       .productName(savedProduct.getName())
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

        transactionItemRepository.saveAndFlush(transactionItem);

        List<TransactionItem> searchedItem = transactionItemRepository.findByTransactionIdAndProductId(savedTransaction.getId(), savedProduct.getId());

        assertEquals(1, searchedItem.size());
        assertEquals(savedProduct.getId(), searchedItem.getFirst().getProduct().getId());
        assertEquals(savedTransaction.getId(), searchedItem.getFirst().getTransaction().getId());
    }

    @Test
    @DisplayName("Should return Optional Empty entity when transaction and product id is not valid")
    public void findByTransactionIdAndProductId_invalidId_returnOptionalEmptyEntity(){
        List<TransactionItem> searchedItem = transactionItemRepository.findByTransactionIdAndProductId(UUID.randomUUID(), 15L);

        assertTrue(searchedItem.isEmpty());
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
            Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
            mockCategorizedProduct.setCategory(null);
            mockCategorizedProduct.setId(null);
            mockCategorizedProduct.setName(mockCategorizedProduct.getName() + i);
            Product savedProduct = productRepository.save(mockCategorizedProduct);

            TransactionItem transactionItem = TransactionItem.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .transaction(savedTransaction)
            .product(savedProduct)
            .productName(savedProduct.getName())
            .price(savedProduct.getBasePrice())
            .quantity(savedProduct.getStockQuantity())
            .build();

            transactionItemRepository.saveAndFlush(transactionItem);
        }
        PageRequest pageable = PageRequest.of(0, 2, Sort.by("createdAt").ascending());
        Slice<TransactionItem> searchedItem = transactionItemRepository.findAllByTransactionId(savedTransaction.getId(), pageable);

        assertEquals(2, searchedItem.getNumberOfElements());
        assertTrue(searchedItem.hasNext());
        assertEquals("Product1", searchedItem.getContent().getLast().getProduct().getName());
    }

    @Test
    @DisplayName("Should return Optional Empty entity when transaction and product id is not valid")
    public void findAllByTransactionId_invalidId_returnOptionalEmptyEntity(){
        Slice<TransactionItem> searchedItem = transactionItemRepository.findAllByTransactionId(UUID.randomUUID(), PageRequest.of(0, 2, Sort.by("createdAt").ascending()));

        assertTrue(searchedItem.isEmpty());
    }

    @Test
    @DisplayName("Should return productsales projection when transaction found")
    public void getProductTopSales_foundData_returnListProjection(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(customer)
        .customerName(customer.getName())
        .build();

       Transaction savedTransaction = transactionRepository.save(transaction);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.save(mockCategorizedProduct);

       TransactionItem transactionItem = TransactionItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .transaction(savedTransaction)
       .product(savedProduct)
       .productName(savedProduct.getName())
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

       TransactionItem transactionItem2 = TransactionItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .transaction(savedTransaction)
       .product(savedProduct)
       .productName(savedProduct.getName())
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity())
       .build();

        transactionItemRepository.saveAllAndFlush(List.of(transactionItem, transactionItem2));

        List<ProductSale> productSales = transactionItemRepository.getProductTopSales(LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertEquals(1, productSales.size());
        assertEquals(savedProduct.getName(), productSales.getFirst().productName());
        assertEquals(transactionItem.getQuantity().add(transactionItem2.getQuantity()).longValue(), productSales.getFirst().totalSold().longValue());
    }

    @Test
    @DisplayName("Should return productrefund projection when transaction found")
    public void getProductTopRefund_foundData_returnListProjection(){
        String invoiceId = invoiceGenerator.generate();

        Transaction transaction = Transaction.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .invoiceId(invoiceId)
        .customer(customer)
        .customerName(customer.getName())
        .build();

       Transaction savedTransaction = transactionRepository.save(transaction);

       Product mockCategorizedProduct = ProductUtils.getMockCategorizedProduct();
       mockCategorizedProduct.setCategory(null);
       mockCategorizedProduct.setId(null);

       Product savedProduct = productRepository.save(mockCategorizedProduct);

       TransactionItem transactionItem = TransactionItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .transaction(savedTransaction)
       .product(savedProduct)
       .productName(savedProduct.getName())
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity().negate())
       .build();

       TransactionItem transactionItem2 = TransactionItem.builder()
       .id(UuidCreator.getTimeOrderedEpochFast())
       .transaction(savedTransaction)
       .product(savedProduct)
       .productName(savedProduct.getName())
       .price(savedProduct.getBasePrice())
       .quantity(savedProduct.getStockQuantity().negate())
       .build();

        transactionItemRepository.saveAllAndFlush(List.of(transactionItem, transactionItem2));

        List<ProductRefund> productSales = transactionItemRepository.getProductTopRefund(LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertEquals(1, productSales.size());
        assertEquals(savedProduct.getName(), productSales.getFirst().productName());
        assertEquals(transactionItem.getQuantity().add(transactionItem2.getQuantity()).longValue(), productSales.getFirst().totalRefunded().longValue());
    }

}
