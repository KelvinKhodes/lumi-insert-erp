package lumi.insert.app.service.supply; 



import lumi.insert.app.core.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.f4b6a3.uuid.UuidCreator;

import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.StockCardRepository;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.core.repository.SupplyItemRepository;
import lumi.insert.app.core.repository.SupplyRepository;
import lumi.insert.app.mapper.AllSupplyMapper;
import lumi.insert.app.service.implement.SupplyServiceImpl;
import lumi.insert.app.utils.generator.InvoiceGenerator;
import lumi.insert.app.utils.generator.JpaSpecGenerator;
import lumi.insert.app.mapper.AllSupplyMapperImpl; 
import lumi.insert.app.mapper.ProductMapperImpl;

@ExtendWith(MockitoExtension.class) 
@ActiveProfiles("test")
public abstract class BaseSupplyServiceTest {
    
    @InjectMocks
    SupplyServiceImpl supplyServiceMock;

    @Mock
    SupplyRepository supplyRepositoryMock;

    @Mock
    ProductRepository productRepositoryMock;

    @Mock
    SupplierRepository supplierRepositoryMock;

    @Mock
    StockCardRepository stockCardRepositoryMock;

    @Mock
    SupplyItemRepository supplyItemRepositoryMock;


    @Mock
    JpaSpecGenerator jpaSpecGenerator;

    @Spy
    AllSupplyMapper allSupplyMapper = new AllSupplyMapperImpl();

    @Spy
    InvoiceGenerator invoiceGenerator = new InvoiceGenerator();

    public Supply setupSupply;

    public SupplyItem setupSupplyItem;

    public Product setupProduct;

    public Supplier setupSupplier;

    public StockCard setupStockCard;

    @BeforeEach
    void setUp(){ 

        setupSupply = Supply.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .build();

        setupSupplyItem = SupplyItem.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .build();

        setupProduct = Product.builder()
        .id(1L)
        .build();

        setupSupplier = Supplier.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .build();

        setupStockCard = StockCard.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
                .build();

        ReflectionTestUtils.setField(allSupplyMapper, "productMapper", new ProductMapperImpl());
    }
}
