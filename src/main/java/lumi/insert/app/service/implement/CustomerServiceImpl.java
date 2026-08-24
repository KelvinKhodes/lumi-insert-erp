package lumi.insert.app.service.implement;
  
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; 

import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.CustomerPicture; 
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.CloudinaryResponse;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.core.repository.CustomerPictureRepository;
import lumi.insert.app.core.repository.CustomerRepository;
import lumi.insert.app.dto.request.CustomerCreateRequest;
import lumi.insert.app.dto.request.CustomerGetByFilter;
import lumi.insert.app.dto.request.CustomerGetNameRequest;
import lumi.insert.app.dto.request.CustomerUpdateRequest;
import lumi.insert.app.dto.response.CustomerDetailResponse;
import lumi.insert.app.dto.response.CustomerNameResponse;
import lumi.insert.app.dto.response.CustomerResponse;
import lumi.insert.app.exception.DatabaseInternalException;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.StorageActionException;
import lumi.insert.app.mapper.CustomerMapper;
import lumi.insert.app.service.CustomerService;
import lumi.insert.app.service.StorageService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

/**
 * Implementation of {@link CustomerService} managing customer lifecycles and associated media.
 * <p>
 * This service provides high-level operations for customer registration, information 
 * updates using MapStruct mappers, complex filtering via JPA Specifications, 
 * and multi-file image management with automated rollback on storage failure.
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class CustomerServiceImpl implements CustomerService{

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    CustomerMapper customerMapper;

    @Autowired
    JpaSpecGenerator jpaSpec;

    @Autowired
    StorageService storageService;

    @Autowired
    CustomerPictureRepository customerPictureRepository;

    /**
     * Registers a new customer.
     *
     * @param request the registration details.
     * @return the detailed response of the registered customer.
     * @throws DuplicateEntityException if the customer name is already registered.
     */
    @Override
    @ActivityLogger(
        entityName = "customers",
        action = ActivityAction.CUSTOMER_REGISTERED,
        actionMessage = "New customer registered"
    )
    public CustomerDetailResponse createCustomer(CustomerCreateRequest request) {
        log.info("Creating customer with name: {}", request.getName());

        if(customerRepository.existsByName(request.getName())) {
            log.debug("Customer creation failed - duplicate name: {}", request.getName());
            throw new DuplicateEntityException("Customer with name " + request.getName() + " already exists");
        }

        Customer customer = Customer.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .name(request.getName())
            .email(request.getEmail())
            .contact(request.getContact())
            .shippingAddress(request.getShippingAddress())
            .build();

        Customer savedCustomer = customerRepository.save(customer);
        log.debug("Customer saved to database: {}", savedCustomer);

        CustomerDetailResponse response = customerMapper.createDtoDetailResponseFromEmployee(savedCustomer);
        log.debug("Customer detail response created: {}", response);

        return response;
    }

    /**
     * Retrieves detailed information of a specific customer.
     *
     * @param id the unique UUID of the customer.
     * @return {@link CustomerDetailResponse} containing full customer profile.
     * @throws NotFoundEntityException if no customer matches the provided UUID.
     */
    @Override
    public CustomerDetailResponse getCustomer(UUID id) {
        log.debug("Getting customer by ID: {}", id);

        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Customer not found with ID: {}", id);
                return new NotFoundEntityException("Customer with id " + id + " is not found");
            });

        log.debug("Customer found: {}", customer);
        CustomerDetailResponse response = customerMapper.createDtoDetailResponseFromEmployee(customer);
        log.debug("Customer detail response created: {}", response);

        return response;
    }

    /**
     * Retrieves a paginated slice of customers based on dynamic JpaSpecification Filtering.
     *
     * @param request the filter and pagination parameters.
     * @return a {@link Slice} of customer summaries.
     */
    @Override
    public Slice<CustomerResponse> getCustomers(CustomerGetByFilter request) {
        log.debug("Getting customers with filter - page: {}, size: {}, name: {}", request.getPage(), request.getSize(), request.getName());

        Pageable pageable = jpaSpec.pageable(request);
        Specification<Customer> customerSpecification = jpaSpec.customerSpecification(request);

        Slice<Customer> customers = customerRepository.findAll(customerSpecification, pageable);
        log.debug("Found {} customers", customers.getNumberOfElements());

        Slice<CustomerResponse> response = customers.map(customerMapper::createDtoResponseFromEmployee);
        log.debug("Customer responses created, total: {}", response.getNumberOfElements());

        return response;
    }

    /**
     * Performs a keyset-paginated search for customer names.
     * <p>Utilizes {@code lastId} to optimize performance for large datasets.</p>
     *
     * @param request search criteria including the partial name and last seen ID.
     * @return a {@link SliceIndex} containing matching names for autocomplete or selection.
     */
    @Override
    public SliceIndex<CustomerNameResponse> searchCustomerNames(CustomerGetNameRequest request) {
        log.debug("Searching customer names with query: {}, size: {}", request.getName(), request.getSize());

        if(request.getLastId() == null) request.setLastId(new UUID(0, 0));
        Pageable pageable = PageRequest.of(0, request.getSize()).withSort(Sort.by("id").ascending());
         
        Slice<CustomerNameResponse> customersName = customerRepository.getByNameContainingIgnoreCaseAndIdAfter(request.getName(), request.getLastId(), pageable);
        log.debug("Found {} customer names", customersName.getNumberOfElements());

        SliceIndex<CustomerNameResponse> response = new SliceIndex<>(customersName);
        log.debug("Customer name search response created");

        return response;
    }

    /**
     * Updates an existing customer's profile information.
     *
     * @param id      the identifier of the customer to update.
     * @param request the partial or full update details.
     * @return the updated {@link CustomerDetailResponse}.
     * @throws DuplicateEntityException if the updated name conflicts with an existing record.
     * @throws NotFoundEntityException  if the target customer does not exist.
     */
    @Override
    @ActivityLogger(
        entityName = "customers",
        action = ActivityAction.CUSTOMER_UPDATED,
        actionMessage = "Customer updated"
    )
    public CustomerDetailResponse updateCustomer(UUID id, CustomerUpdateRequest request) {
        log.info("Updating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Customer not found for update with ID: {}", id);
                return new NotFoundEntityException("Customer with id " + id + " is not found");
            });

        if(request.getName() != null && !customer.getName().contains(request.getName()) ) {
            if(customerRepository.existsByName(request.getName())){
                log.debug("Customer update failed - duplicate name: {}", request.getName());
                throw new DuplicateEntityException("Customer with name " + request.getName() + " already exists");
            }
        }

        customerMapper.updateEntityFromDto(request, customer);
        log.debug("Customer updated in database: {}", customer);

        CustomerDetailResponse response = customerMapper.createDtoDetailResponseFromEmployee(customer);
        log.debug("Customer detail response created: {}", response);

        return response;
    }

    /**
     * Uploads multiple pictures and associates them with a customer.
     * <p>
     * This method implements a manual compensation logic: if the database persistence fails 
     * after images are uploaded to the cloud storage, it attempts to delete the orphaned 
     * cloud assets to maintain consistency.
     * </p>
     *
     * @param id    the customer identifier.
     * @param files array of multipart files to be uploaded.
     * @return {@code true} if all files were successfully uploaded and persisted.
     * @throws StorageActionException    if a network or I/O error occurs during upload.
     * @throws DatabaseInternalException if persistence fails, triggering the cleanup process.
     */
    @Override
    public Boolean addCustomerPicture(UUID id, MultipartFile[] files) {
        log.info("Adding pictures to customer with ID: {}, file count: {}", id, files.length);

        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Customer not found for picture upload with ID: {}", id);
                return new NotFoundEntityException("Customer with id " + id + " is not found");
            });
            
        List<String> pictureUrl = customer.getPictureUrl();
        if(pictureUrl == null) pictureUrl = new ArrayList<>();

        log.debug("Customer found for picture upload: {}", customer.getName());
        String fileName = customer.getName() + "-";
        List<CustomerPicture> customerPictures = new ArrayList<>();
        List<String> picturesId = new ArrayList<>();
        try {
            for (MultipartFile file : files) { 
                CloudinaryResponse upload = storageService.uploadImageSync(file.getBytes(), fileName + LocalDateTime.now() , "customer");
                log.debug("Image uploaded successfully: {}", upload.getSecureUrl());

                CustomerPicture customerPicture = CustomerPicture.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .pictureUrl(upload.getSecureUrl())
                .customer(customer)
                .publicId(upload.getPublicId())
                .build(); 

                customerPictures.add(customerPicture);
                picturesId.add(upload.getPublicId());
                pictureUrl.add(upload.getSecureUrl()); 
            }
            customer.setPictureUrl(pictureUrl);
            customerPictureRepository.saveAll(customerPictures);
            log.debug("All customer pictures saved to database");

            log.info("Successfully added {} pictures to customer: {}", files.length, customer.getName());
            return true;

        } catch (IOException e) {
            log.error("Upload failed for customer {} with {} files, messages: {}", customer.getName(), files.length, e.getMessage());
            log.debug("IOException during picture upload", e);
            throw new StorageActionException("Server couldn't complete the request due to internal problem, try again or contact developer");
        } catch (Exception e) { 
            log.error("Save to database failed for customer {} with {} files, attempting to delete images at storage. Messages: {}", customer.getName(), files.length, e.getMessage()); 
            log.debug("Exception during database save", e);

            picturesId.forEach(pictureId -> {
                 if(!(storageService.deleteImage(pictureId))) log.error("Failed to delete image with publicId: {}", pictureId);
            }); 

            throw new DatabaseInternalException("Server couldn't complete the request due to internal problem, try again or contact developer");
        } 
    }

}
