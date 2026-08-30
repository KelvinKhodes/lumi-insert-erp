package lumi.insert.app.service.implement;
 

import lumi.insert.app.dto.request.CategoryGetRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.Category;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.repository.CategoryRepository;
import lumi.insert.app.dto.request.CategoryCreateRequest;
import lumi.insert.app.dto.request.CategoryUpdateRequest;
import lumi.insert.app.dto.request.PaginationRequest;
import lumi.insert.app.dto.response.CategoryResponse;
import lumi.insert.app.exception.BoilerplateRequestException;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.mapper.CategoryMapper;
import lumi.insert.app.service.CategoryService;

/**
 * Services of {@link Category}.
 * <p>Handles category management such as creation, update, etc.</p>
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Service
@Transactional
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CategoryMapper categoryMapper;

    /**
     * Creates a new category.
     * * @param request the category creation details.
     * @return the mapped {@link CategoryResponse} of the saved entity.
     * @throws DuplicateEntityException if a category with the same name already exists.
     */
    @Override
    @ActivityLogger(
        entityName = "categories",
        action = ActivityAction.CATEGORY_CREATED,
        actionMessage = "Category created"
    )
    @CacheEvict(
        value = "categories:first-page", allEntries = true
    )
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        log.info("Creating category with name: {}", request.getName());

        if(categoryRepository.existsByName(request.getName())){
            throw  new DuplicateEntityException("Category with name " + request.getName() + " already exists");
        } else {
            Category newCategory = Category.builder()
                .name(request.getName())
                .build();

            Category savedCategory = categoryRepository.save(newCategory);
            log.debug("Category saved to database: {}", savedCategory);

            CategoryResponse response = categoryMapper.createDtoResponseFromCategory(savedCategory);
            log.debug("Category response created: {}", response);

            return response;
        }
    }

    /**
     * Updates the name of an existing category.
     * * @param request the update request containing the ID and the new name.
     * @return the mapped {@link CategoryResponse}.
     * @throws DuplicateEntityException if the new name is already taken.
     * @throws NotFoundEntityException if the category ID does not exist.
     */
    @Override 
    @ActivityLogger(
        entityName = "categories",
        action = ActivityAction.CATEGORY_UPDATED,
        actionMessage = "Category name updated"
    )
    @CacheEvict(
        value = "categories:first-page", allEntries = true
    )
    public CategoryResponse updateCategoryName(CategoryUpdateRequest request) {
        log.info("Updating category name for ID: {} to: {}", request.getId(), request.getName());

        if(categoryRepository.existsByName(request.getName())){
            throw  new DuplicateEntityException("Category with name " + request.getName() + " already exists");
        } else {
        Category searchedCategory = categoryRepository.findById(request.getId()).orElseThrow(() -> new NotFoundEntityException("Category with ID " + request.getId() + " was not found"));
        
        searchedCategory.setName(request.getName());
        Category savedCategory = categoryRepository.save(searchedCategory);
        log.debug("Category updated in database: {}", savedCategory);

        CategoryResponse response = categoryMapper.createDtoResponseFromCategory(savedCategory);
        log.debug("Category response created: {}", response);
        return response;
        }
    }

    /**
     * Sets a category status to active.
     * * @param id the unique identifier of the category.
     * @return the updated {@link CategoryResponse}.
     * @throws NotFoundEntityException if the category ID is not found.
     * @throws BoilerplateRequestException if the category is already active.
     */
    @Override
    @ActivityLogger(
        entityName = "categories",
        action = ActivityAction.CATEGORY_UPDATED,
        actionMessage = "Category status set to active"
    )
    @CacheEvict(
        value = "categories:first-page", allEntries = true
    )
    public CategoryResponse activateCategory(Long id) {
        log.info("Activating category with ID: {}", id);

        Category searchedCategory = categoryRepository.findById(id).orElseThrow(() -> new NotFoundEntityException("Category with ID " + id + " was not found"));
        if(searchedCategory.getIsActive()) throw new BoilerplateRequestException("Category with ID " + id + " already active");

        searchedCategory.setIsActive(true);
        Category savedCategory = categoryRepository.save(searchedCategory);
        log.debug("Category activated in database: {}", savedCategory);

        CategoryResponse response = categoryMapper.createDtoResponseFromCategory(savedCategory);
        log.debug("Category response created: {}", response);
        return response;
    }

    /**
     * Sets a category status to inactive.
     * * @param id the unique identifier of the category.
     * @return the updated {@link CategoryResponse}.
     * @throws NotFoundEntityException if the category ID is not found.
     * @throws BoilerplateRequestException if the category is already inactive.
     */
    @Override
    @ActivityLogger(
        entityName = "categories",
        action = ActivityAction.CATEGORY_UPDATED,
        actionMessage = "Category status set to inactive"
    )
    @CacheEvict(
        value = "categories:first-page", allEntries = true
    )
    public CategoryResponse deactivateCategory(Long id) {
        log.info("Deactivating category with ID: {}", id);

        Category searchedCategory = categoryRepository.findById(id).orElseThrow(() -> new NotFoundEntityException("Category with ID " + id + " was not found"));
        if(!searchedCategory.getIsActive()) throw new BoilerplateRequestException("Category with ID " + id + " already inactive");

        searchedCategory.setIsActive(false);
        Category savedCategory = categoryRepository.save(searchedCategory);
        log.debug("Category deactivated in database: {}", savedCategory);
        
        CategoryResponse response = categoryMapper.createDtoResponseFromCategory(savedCategory);
        log.debug("Category response created: {}", response);
        return response;
    }

    /**
     * Retrieves a single category by its ID.
     * * @param id the unique identifier.
     * @return the found {@link CategoryResponse}.
     * @throws NotFoundEntityException if no category matches the given ID.
     */
    @Override
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Getting category by ID: {}", id);

        Category searchedCategory = categoryRepository.findById(id).orElseThrow(() -> new NotFoundEntityException("Category with ID " + id + " was not found"));
        log.debug("Category found: {}", searchedCategory);

        CategoryResponse response = categoryMapper.createDtoResponseFromCategory(searchedCategory);
        log.debug("Category response created: {}", response);

        return response;
    }

    /**
     * Retrieves a paginated slice of active categories.
     * <p>Results are sorted by name in ascending order by default.</p>
     * * @param request the pagination parameters (page and size).
     * @return a {@link Slice} of {@link CategoryResponse} objects.
     */
    @Override
    @Cacheable(
        value = "categories:first-page",
        key = "#request.sortBy + '_' + #request.sortDirection + '_' + #request.getSize",
        condition = "#request.getPage == 0 && (#request.getSize == 100 || #request.getSize == 12) " +
            "&& #request.isArchived == false"
    )
    public Slice<CategoryResponse> getCategories(CategoryGetRequest request) {
        log.debug("Getting categories with pagination - page: {}, size: {}", request.getPage(), request.getSize());

        Sort sort = Sort.by("name").ascending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize()).withSort(sort);

        Slice<Category> searchedCategories = request.getIsArchived() ? categoryRepository.findAllByIsActiveFalse(pageable) :  categoryRepository.findAllByIsActiveTrue(pageable);
        log.debug("Found {} categories", searchedCategories.getNumberOfElements());

        Slice<CategoryResponse> response = searchedCategories.map(categoryMapper::createDtoResponseFromCategory);
        log.debug("Category responses created, total: {}", response.getNumberOfElements());

        return response;
    }
    
}

 