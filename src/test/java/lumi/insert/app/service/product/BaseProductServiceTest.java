package lumi.insert.app.service.product;

import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.nondatabase.CloudinaryResponse;
import lumi.insert.app.core.repository.ProductPictureRepository;
import lumi.insert.app.service.implement.CloudinaryStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lumi.insert.app.TestContainerTest;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;
import lumi.insert.app.core.repository.CategoryRepository;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.mapper.ProductMapper;
import lumi.insert.app.service.ProductService;
import lumi.insert.app.service.implement.ProductServiceImpl; 
import lumi.insert.app.mapper.CategoryMapperImpl;
import lumi.insert.app.mapper.ProductMapperImpl;

@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
public abstract class BaseProductServiceTest extends TestContainerTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductService productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CacheManager cacheManager;

    @InjectMocks
    ProductServiceImpl productServiceMock;

    @Mock
    ProductRepository productRepositoryMock;

    @Mock
    CategoryRepository categoryRepositoryMock;

    @Mock
    RedisConnectionFactory redisConnectionFactory;

    @Mock
    CloudinaryStorageServiceImpl storageService;

    @Mock
    ProductPictureRepository productPictureRepository;
 
    @Spy 
    ProductMapper productMapper = new ProductMapperImpl();

    Product setupProduct;

    CloudinaryResponse cloudinaryResponse = CloudinaryResponse.builder()
        .secureUrl("testUrl.test")
        .publicId("id123")
        .build();

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
        }
        if (cacheManager.getCache("products:first-page") != null) {
            cacheManager.getCache("products:first-page").clear();
        }

        EmployeeLogin employeeLogin = EmployeeLogin.builder()
        .id(UuidCreator.getTimeOrderedEpochFast())
        .username("Test Username")
        .role(EmployeeRole.CASHIER)
        .ipAddress("t.e.s.t")
        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(employeeLogin, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        ReflectionTestUtils.setField(productMapper, "categoryMapper", new CategoryMapperImpl());

        setupProduct = Product.builder()
            .id(1L)
            .name("Shoes")
            .build();
    }

}
