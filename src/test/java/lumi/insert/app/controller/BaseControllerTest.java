package lumi.insert.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import lumi.insert.app.mapper.*;
import lumi.insert.app.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import lumi.insert.app.TestContainerTest;
import lumi.insert.app.service.implement.StockCardServiceImpl;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest 
@WithMockUser(username = "admin")
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public abstract class BaseControllerTest extends TestContainerTest{

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected AuthTokenService authTokenService;

    @MockitoBean
    protected CategoryService categoryService;

    @MockitoBean
    protected CustomerService customerService;

    @MockitoBean
    protected EmployeeService employeeService;

    @MockitoBean
    protected MemoService memoService;

    @MockitoBean
    protected ProductService productService;

    @MockitoBean
    protected PdfService pdfService;

    @MockitoBean
    protected TransactionItemService transactionItemService;

    @MockitoBean
    protected StockCardServiceImpl stockCardService;

    @MockitoBean
    protected SupplierService supplierService;

    @MockitoBean
    protected SupplyService supplyService;

    @MockitoBean
    protected XlsxService xlsxService;
    
    @MockitoBean
    protected SupplyPaymentService supplyPaymentService;

    @MockitoBean
    protected TransactionService transactionService; 

    @MockitoBean
    protected TransactionPaymentService transactionPaymentService;

    @MockitoBean
    protected ActivityLogService activityLogService;

    @Autowired
    protected AllTransactionMapper allTransactionMapper;

    @Autowired
    protected AllSupplyMapper allSupplyMapper;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected CategoryMapper categoryMapper;
    
    @Autowired
    protected ProductMapperImpl productMapper;

    @Autowired
    protected ActivityLogMapper activityLogMapper;

    @BeforeEach
    void setup(WebApplicationContext context) {
//    mockMvc = MockMvcBuilders
//            .webAppContextSetup(context)
////            .apply(SecurityMockMvcConfigurers.springSecurity())
//            .build();

     when(redisTemplate.opsForValue()).thenReturn(valueOperations);
     when(valueOperations.get(any())).thenReturn(null);
    }

}
