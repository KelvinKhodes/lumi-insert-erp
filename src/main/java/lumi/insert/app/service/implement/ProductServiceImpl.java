package lumi.insert.app.service.implement;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.f4b6a3.uuid.UuidCreator;
import lumi.insert.app.core.entity.*;
import lumi.insert.app.core.entity.nondatabase.CloudinaryResponse;
import lumi.insert.app.core.entity.nondatabase.EmployeeLogin;
import lumi.insert.app.core.repository.ProductPictureRepository;
import lumi.insert.app.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
 
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.core.repository.CategoryRepository;
import lumi.insert.app.core.repository.ProductRepository;
import lumi.insert.app.core.repository.projection.ProductOutOfStock;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.request.ProductCreateRequest;
import lumi.insert.app.dto.request.ProductUpdateRequest;
import lumi.insert.app.dto.request.ProductGetByFilter;
import lumi.insert.app.dto.request.ProductGetNameRequest;
import lumi.insert.app.dto.response.ProductDeleteResponse;
import lumi.insert.app.dto.response.ProductName;
import lumi.insert.app.dto.response.ProductResponse;
import lumi.insert.app.dto.response.ProductStockResponse;
import lumi.insert.app.mapper.ProductMapper;
import lumi.insert.app.service.ProductService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of {@link ProductService} for comprehensive inventory and product management.
 * <p>
 * This service ensures strict inventory consistency by synchronizing product states with 
 * {@link Category} statistics. It handles complex lifecycle operations such as 
 * dynamic category reassignment, stock monitoring, and advanced filtering using 
 * JPA Specifications and keyset pagination.
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductMapper productMapper;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    @Autowired
    CloudinaryStorageServiceImpl storageService;

    @Autowired
    ProductPictureRepository productPictureRepository;

    /**
     * Creates a new product and increments the associated category's item count.
     *
     * @param request the product details including pricing and initial stock.
     * @return the mapped {@link ProductResponse}.
     * @throws DuplicateEntityException if a product with the same name already exists.
     * @throws NotFoundEntityException  if the specified category ID does not exist.
     */
    @Override
    @ActivityLogger(
        entityName = "products",
        action = ActivityAction.PRODUCT_CREATED,
        actionMessage = "New product created"
    )
    @Caching(
        put = {
            @CachePut(value = "products", key = "#result.id")
        },
        evict = {
            @CacheEvict(value = "products:first-page", allEntries = true)
        }
    )
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating new product with name: {}", request.getName());
        if (productRepository.existsByName(request.getName())) {
            log.debug("Product creation failed - duplicate name: {}", request.getName());
            throw new DuplicateEntityException("Product with name " + request.getName() + " already exists");
        }

        Product newProduct = Product.builder()
            .name(request.getName())
            .basePrice(request.getBasePrice())
            .sellPrice(request.getSellPrice())
            .stockQuantity(request.getStockQuantity())
            .build();

        if(request.getStockMinimum() != null) {
            newProduct.setStockMinimum(request.getStockMinimum());
        }

        if(request.getCategoryId() != null) {
            log.debug("Product creation includes category ID: {}", request.getCategoryId());
            Category searchedCategory = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> {
                log.debug("Category not found for product creation with ID: {}", request.getCategoryId());
                return new NotFoundEntityException("Category with ID " + request.getCategoryId() + " was not found");
            });

            newProduct.setCategory(searchedCategory);
            searchedCategory.setTotalItems(searchedCategory.getTotalItems() + 1);
            categoryRepository.save(searchedCategory); 
        }

        Product savedProduct = productRepository.save(newProduct);
        log.debug("Product saved to database: {}", savedProduct);

        ProductResponse dtoResponseFromProduct = productMapper.createDtoResponseFromProduct(savedProduct);
        log.debug("Product response created: {}", dtoResponseFromProduct);
        return dtoResponseFromProduct;
    }


    /**
     * Retrieves the current stock quantity for a specific product.
     *
     * @param productId the unique identifier of the product.
     * @return a {@link ProductStockResponse} containing the ID and current stock level.
     * @throws NotFoundEntityException if the product ID is not found.
     */
    @Override
    public ProductStockResponse getProductStock(Long productId) {
        log.debug("Getting stock for product ID: {}", productId);
        BigDecimal stock = productRepository.getStockById(productId).orElseThrow(() -> {
            log.debug("Product stock not found for ID: {}", productId);
            return new NotFoundEntityException("Product with ID " + productId + " was not found");
        });

        ProductStockResponse responseStock = ProductStockResponse.builder()
            .id(productId)
            .stockQuantity(stock)
            .build();

        return responseStock;
    } 

    /**
     * Updates product information and handles category migration logic.
     * <p>
     * If the category is changed, the service automatically decrements the item count 
     * from the old category and increments the count for the new category.
     * </p>
     *
     * @param request the update data containing the product ID and new values.
     * @return the updated {@link ProductResponse}.
     * @throws NotFoundEntityException if the product or the new category is not found.
     */
    @Override
    @ActivityLogger(
        entityName = "products",
        action = ActivityAction.PRODUCT_UPDATED,
        actionMessage = "Product updated"
    )
    @Caching(
        put = {
            @CachePut(value = "products", key = "#result.id")
        },
        evict = {
            @CacheEvict(value = "products:first-page", allEntries = true)
        }
    )
    public ProductResponse updateProduct(ProductUpdateRequest request) {
        log.info("Updating product with ID: {}", request.getId());
        Product existingProduct = productRepository.findById(request.getId()).orElseThrow(() -> {
            log.debug("Product not found for update with ID: {}", request.getId());
            return new NotFoundEntityException("Product with ID " + request.getId() + " was not found");
        });

        productMapper.updateProductFromDto(request, existingProduct);
        Category category = existingProduct.getCategory();
        Long newCategoryId = request.getCategoryId();

        if(newCategoryId != null && (category == null || !category.getId().equals(newCategoryId))){
            if(category != null) {
                category.setTotalItems(category.getTotalItems() - 1L);
                categoryRepository.save(category);
            }

            Category newCategory = categoryRepository.findById(newCategoryId).orElseThrow(() -> {
                log.debug("Category not found for product update with ID: {}", newCategoryId);
                return new NotFoundEntityException("Category with ID " + newCategoryId + " was not found");
            });
            existingProduct.setCategory(newCategory);
            newCategory.setTotalItems(newCategory.getTotalItems() + 1L);

            categoryRepository.save(newCategory);
        } 

        Product updatedProduct = productRepository.save(existingProduct);

        ProductResponse dtoResponseFromProduct = productMapper.createDtoResponseFromProduct(updatedProduct);
        log.debug("Product updated and response created: {}", dtoResponseFromProduct);
        return dtoResponseFromProduct;
    }

    /**
     * Performs a keyset-paginated search for active product names.
     *
     * @param request search parameters including partial name and last seen ID.
     * @return a {@link SliceIndex} of matching product names.
     */
    @Override
    public SliceIndex<ProductName> searchProductNames(ProductGetNameRequest request) {
        log.debug("Searching product names with query: {}, size: {}", request.getName(), request.getSize());
        if(request.getLastId() == null) request.setLastId(0L);
        Pageable pageable = PageRequest.of(0, request.getSize()).withSort(Sort.by("id").ascending());

        Slice<ProductName> allByNameContaining = productRepository.getByNameContainingIgnoreCaseAndIsActiveTrueAndIdAfter(request.getName(), request.getLastId(), pageable);
        log.debug("Found {} product names", allByNameContaining.getNumberOfElements());
        return new SliceIndex<ProductName>(allByNameContaining);
    }

    /**
     * Retrieves a single product's details by its ID.
     *
     * @param id the product identifier.
     * @return the found {@link ProductResponse}.
     */
    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.debug("Getting product by ID: {}", id);
        Product searchedProduct = productRepository.findById(id).orElseThrow(() -> {
            log.debug("Product not found with ID: {}", id);
            return new NotFoundEntityException("Product with ID " + id + " was not found");
        });
 
        ProductResponse responseProduct = productMapper.createDtoResponseFromProduct(searchedProduct);
        log.debug("Product response created: {}", responseProduct);
        return responseProduct;
    }

    /**
     * Retrieves a paginated slice of all products sorted by name.
     *
     * @param request pagination parameters (page, size).
     * @return a {@link Slice} of product responses.
     */
    @Override
    public Slice<ProductResponse> getProducts(PaginationRequest request) {
        log.debug("Getting products with pagination page: {}, size: {}", request.getPage(), request.getSize());
        Sort sort = Sort.by("name").ascending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize()).withSort(sort);

        Slice<Product> allRawProducts = productRepository.findAllBy(pageable);
        log.debug("Found {} products", allRawProducts.getNumberOfElements());
        Slice<ProductResponse> mapResult = allRawProducts.map(productMapper::createDtoResponseFromProduct);
        return mapResult;
    }

    /**
     * Searches for products using dynamic multi-criteria filters.
     *
     * @param request complex filter criteria (price range, category, status, etc.).
     * @return a filtered {@link Slice} of products.
     * @throws NotFoundEntityException if a category filter is provided but the category doesn't exist.
     */
    @Override
    @Cacheable(
        value = "products:first-page",
        key = "#request.sortBy + '_' + #request.sortDirection + '_' + #request.getSize",
        condition = "#request.categoryId == null " +
            "&& (#request.getPage == 0 && (#request.getSize == 10 || #request.getSize == 5)) " +
            "&& (#request.name == null || #request.name.isEmpty()) " +
            "&& (#request.minPrice == null || #request.minPrice.compareTo(T(java.math.BigDecimal).ZERO) == 0) " +
            "&& (#request.maxPrice == null || #request.maxPrice.compareTo(T(java.math.BigDecimal).valueOf(50000000L)) == 0)"
    )
    public SliceIndex<ProductResponse> getProductsByRequests(ProductGetByFilter request) {
        log.debug("Searching products by filter: {}", request);
        if(request.getCategoryId() != null && !(categoryRepository.existsById(request.getCategoryId()))){
            log.debug("Category filter ID not found: {}", request.getCategoryId());
            throw new NotFoundEntityException("Category with ID " + request.getCategoryId() + " was not found");
        }
        Pageable pageable = jpaSpecGenerator.pageable(request);
        Specification<Product> productSpecification = jpaSpecGenerator.productSpecification(request);

        Slice<Product> result = productRepository.findAll(productSpecification, pageable);
        log.debug("Found {} filtered products", result.getNumberOfElements());
        Slice<ProductResponse> resultMap = result.map(productMapper::createDtoResponseFromProduct);
        return new SliceIndex<>(resultMap);
    }

    /**
     * Deactivates a product and decrements the associated category's active item count.
     *
     * @param id the product identifier.
     * @return the deactivation metadata.
     * @throws BoilerplateRequestException if the product is already inactive.
     */
    @Override
    @ActivityLogger(
        entityName = "products",
        action = ActivityAction.PRODUCT_UPDATED,
        actionMessage = "Product set to inactive"
    )
    @Caching(
        evict = {
            @CacheEvict(value = "products:first-page", allEntries = true),
            @CacheEvict(value = "products", key = "#id")
        }
    )
    public ProductDeleteResponse deactivateProduct(Long id) {
        log.info("Deactivating product with ID: {}", id);
        Product searchedProduct = productRepository.findById(id).orElseThrow(() -> {
            log.debug("Product not found for deactivation with ID: {}", id);
            return new NotFoundEntityException("Category with ID " + id + " was not found");
        });
        if(!searchedProduct.getIsActive()) {
            log.debug("Product already inactive with ID: {}", id);
            throw new BoilerplateRequestException("Product with ID " + id + " already inactive");
        }

        Category category = searchedProduct.getCategory();

        if(category != null){
            category.setTotalItems(category.getTotalItems() - 1L);
            categoryRepository.save(category);
        }

        searchedProduct.setIsActive(false);
        Product savedProduct = productRepository.save(searchedProduct);

        ProductDeleteResponse deleteDtoResponseFromProduct = productMapper.createDeleteDtoResponseFromProduct(savedProduct);
        log.debug("Product deactivated and response created: {}", deleteDtoResponseFromProduct);
        return deleteDtoResponseFromProduct;
    }

    /**
     * Reactivates a product and increments the associated category's active item count.
     *
     * @param id the product identifier.
     * @return the activation metadata.
     * @throws BoilerplateRequestException if the product is already active.
     */
    @Override
    @ActivityLogger(
        entityName = "products",
        action = ActivityAction.PRODUCT_UPDATED,
        actionMessage = "Product set to active"
    )
    @Caching(
        evict = {
            @CacheEvict(value = "products:first-page", allEntries = true),
            @CacheEvict(value = "products", key = "#id")
        }
    )
    public ProductDeleteResponse activateProduct(Long id) {
        log.info("Activating product with ID: {}", id);
        Product searchedProduct = productRepository.findById(id).orElseThrow(() -> {
            log.debug("Product not found for activation with ID: {}", id);
            return new NotFoundEntityException("Product with ID " + id + " was not found");
        });
        if(searchedProduct.getIsActive()) {
            log.debug("Product already active with ID: {}", id);
            throw new BoilerplateRequestException("Product with ID " + id + " already active");
        }

        Category category = searchedProduct.getCategory();

            if(category != null){
                category.setTotalItems(category.getTotalItems() + 1L);
                categoryRepository.save(category);
            }
        searchedProduct.setIsActive(true);
        Product savedProduct = productRepository.save(searchedProduct);

        ProductDeleteResponse deleteDtoResponseFromProduct = productMapper.createDeleteDtoResponseFromProduct(savedProduct);
        log.debug("Product activated and response created: {}", deleteDtoResponseFromProduct);
        return deleteDtoResponseFromProduct;
    }

    /**
     * Identifies products whose stock levels have fallen below their defined minimum threshold.
     *
     * @return a list of products requiring replenishment.
     */
    @Override
    public List<ProductOutOfStock> getOutOfStockProducts() {
        log.debug("Fetching out-of-stock products");
        List<ProductOutOfStock> outOfStockProducts = productRepository.findAllOutOfStockProduct();
        log.debug("Found {} out-of-stock products", outOfStockProducts.size());
        return outOfStockProducts;
    }

    /**
     * Uploads multiple pictures and associates them with a product.
     * <p>
     * This method implements a manual compensation logic: if the database persistence fails
     * after images are uploaded to the cloud storage, it attempts to delete the orphaned
     * cloud assets to maintain consistency.
     * </p>
     *
     * @param id    the product identifier.
     * @param files array of multipart files to be uploaded.
     * @return {@code true} if all files were successfully uploaded and persisted.
     * @throws StorageActionException    if a network or I/O error occurs during upload.
     * @throws DatabaseInternalException if persistence fails, triggering the cleanup process.
     */
    // Implement parallel stream and attempt values to this method
    @Caching(
        evict = {
            @CacheEvict(value = "products", key = "#id")
        }
    )
    @Override
    public String uploadProductPictures(Long id, MultipartFile[] files) {
        log.info("Uploading {} pictures for Product id={}", files.length, id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Pictures upload failed, pproduct not found id={}", id);
                return new NotFoundEntityException("Product with ID " + id + " was not found");
            });

        AtomicInteger failedAttempt = new AtomicInteger(0);

        List<String> pictureUrl = Collections.synchronizedList(new ArrayList<>());

        SecurityContext authContext = Optional.of(SecurityContextHolder.getContext())
            .orElseThrow(() -> new BadCredentialsException(""));

        Arrays.stream(files).parallel().forEach(file -> {
            String publicId = null;
            String fileName = "product-pictures-" + UuidCreator.getTimeOrderedEpochFast();

            try {
                SecurityContextHolder.setContext(authContext);
                CloudinaryResponse upload = storageService.uploadImageSync(file.getBytes(), fileName ,"product");

                ProductPicture productPicture = ProductPicture.builder()
                    .id(UuidCreator.getTimeOrderedEpochFast())
                    .pictureUrl(upload.getSecureUrl())
                    .product(product)
                    .publicId(upload.getPublicId())
                    .build();

                publicId = upload.getPublicId();

                productPictureRepository.save(productPicture);
                pictureUrl.add(upload.getSecureUrl());
                log.info("Product picture uploaded for ID={}, publicId={}", id, publicId);
            } catch (IOException e) {
                failedAttempt.incrementAndGet();
                log.error("Upload failed for productId={}, messages={}", id, e.getMessage());
//                throw new StorageActionException("Server couldn't complete the request due to internal problem, try again or contact developer");
            } catch (Exception e) {
                log.error("Save to database failed for productId={}, attempting to delete image at storage. Messages={}", id, e.getMessage());

                if (publicId != null) {
                    if (!(storageService.deleteImage(publicId)))
                        log.error("Failed to delete image with publicId: {}", publicId);
                }
                throw new DatabaseInternalException("Server couldn't complete the request due to internal problem, try again or contact developer");
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        int failed = failedAttempt.get();

        if(!pictureUrl.isEmpty()) product.getPictureUrl().addAll(pictureUrl);

        if(failed == files.length){
            throw new StorageActionException("Server couldn't complete the request due to internal problem, try again or contact developer");
        } else if (failed > 0 && failed < files.length) {
            return failed + " pictures failed to upload due to internal or provider problem, check your uploaded pictures and retry again.";
        } else {
            return "Upload product's pictures completed successfully for product with ID: " + id;
        }

    }

}
