package lumi.insert.app.service.implement;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;
import lumi.insert.app.core.entity.nondatabase.SliceIndex;
import lumi.insert.app.core.repository.SupplierRepository;
import lumi.insert.app.dto.request.SupplierCreateRequest;
import lumi.insert.app.dto.request.SupplierGetByFilter;
import lumi.insert.app.dto.request.SupplierGetNameRequest;
import lumi.insert.app.dto.request.SupplierUpdateRequest;
import lumi.insert.app.dto.response.SupplierDetailResponse;
import lumi.insert.app.dto.response.SupplierNameResponse;
import lumi.insert.app.exception.DuplicateEntityException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.mapper.SupplierMapper;
import lumi.insert.app.service.SupplierService;
import lumi.insert.app.utils.generator.JpaSpecGenerator;

/**
 * Implementation of {@link SupplierService} managing the procurement-side partner registry.
 * <p>
 * This service facilitates the management of supplier entities, ensuring unique identification 
 * via time-ordered UUIDs. It supports dynamic filtering for administrative interfaces 
 * and optimized keyset pagination for high-speed name lookups.
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Transactional
@Slf4j
public class SupplierServiceImpl implements SupplierService{

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    SupplierMapper supplierMapper;

    @Autowired
    JpaSpecGenerator jpaSpecGenerator;

    /**
     * Registers a new procurement supplie.
     *
     * @param request the supplier registration details.
     * @return the detailed response of the created supplier.
     * @throws DuplicateEntityException if a supplier with the same name already exists.
     */
    @Override
    @ActivityLogger(
        entityName = "suppliers",
        action = ActivityAction.SUPPLIER_REGISTERED,
        actionMessage = "New supplier registered"
    )
    public SupplierDetailResponse createSupplier(SupplierCreateRequest request) {
        log.info("Creating supplier with name: {}", request.getName());
        if(supplierRepository.existsByName(request.getName())) {
            log.debug("Supplier creation failed - duplicate name: {}", request.getName());
            throw new DuplicateEntityException("Supplier with name " + request.getName() + " already exists");
        }

        Supplier supplier = Supplier.builder()
            .id(UuidCreator.getTimeOrderedEpochFast())
            .name(request.getName())
            .email(request.getEmail())
            .contact(request.getContact()) 
            .build();

        Supplier savedSupplier = supplierRepository.save(supplier);
        log.debug("Supplier saved to database: {}", savedSupplier);
        SupplierDetailResponse response = supplierMapper.createDtoDetailResponseFromSupplier(savedSupplier);
        log.debug("Supplier response created: {}", response);
        return response;
    }

    /**
     * Retrieves the complete profile of a supplier by their UUID.
     *
     * @param id the unique identifier of the supplier.
     * @return {@link SupplierDetailResponse} containing contact and identity info.
     * @throws NotFoundEntityException if the supplier record is missing.
     */
    @Override
    public SupplierDetailResponse getSupplier(UUID id) {
        log.debug("Getting supplier by ID: {}", id);
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Supplier not found with ID: {}", id);
                return new NotFoundEntityException("Supplier with id " + id + " is not found");
            });

        SupplierDetailResponse response = supplierMapper.createDtoDetailResponseFromSupplier(supplier);
        log.debug("Supplier response created: {}", response);
        return response;
    }

    /**
     * Retrieves a paginated slice of suppliers based on flexible search criteria.
     * <p>Utilizes {@link JpaSpecGenerator} to handle complex multi-field filtering.</p>
     *
     * @param request the filter and pagination parameters.
     * @return a {@link Slice} of supplier profiles.
     */
    @Override
    public Slice<SupplierDetailResponse> getSuppliers(SupplierGetByFilter request) {
        log.debug("Getting suppliers with filter: {}", request);
        Pageable pageable = jpaSpecGenerator.pageable(request);

        Specification<Supplier> supplierSpecification = jpaSpecGenerator.supplierSpecification(request);

        Slice<Supplier> suppliers = supplierRepository.findAll(supplierSpecification, pageable);
        log.debug("Found {} suppliers", suppliers.getNumberOfElements());
        return suppliers.map(supplierMapper::createDtoDetailResponseFromSupplier);
    }

    /**
     * Searches for supplier names using high-performance keyset pagination.
     * <p>
     * Designed for autocomplete or selection components where performance is critical. 
     * Uses {@code lastId} to maintain consistent ordering and low latency.
     * </p>
     *
     * @param request search query and keyset metadata.
     * @return a {@link SliceIndex} of matching supplier names.
     */
    @Override
    public SliceIndex<SupplierNameResponse> searchSupplierNames(SupplierGetNameRequest request) {
        log.debug("Searching supplier names with query: {}, size: {}", request.getName(), request.getSize());
        if(request.getLastId() == null) request.setLastId(new UUID(0, 0));
        Pageable pageable = PageRequest.of(0, request.getSize()).withSort(Sort.by("id").ascending());
        
        Slice<SupplierNameResponse> suppliersName = supplierRepository.getByNameContainingIgnoreCaseAndIdAfter(request.getName(), request.getLastId(), pageable);
        log.debug("Found {} supplier names", suppliersName.getNumberOfElements());
        return new SliceIndex<SupplierNameResponse>(suppliersName);
    }

    /**
     * Updates an existing supplier's information.
     *
     * @param id      the unique identifier of the target supplier.
     * @param request the update data.
     * @return the updated {@link SupplierDetailResponse}.
     * @throws DuplicateEntityException if the updated name conflicts with another supplier.
     * @throws NotFoundEntityException  if the supplier is not found.
     */
    @Override
    @ActivityLogger(
        entityName = "suppliers",
        action = ActivityAction.SUPPLIER_UPDATED,
        actionMessage = "Supplier updated"
    )
    public SupplierDetailResponse updateSupplier(UUID id, SupplierUpdateRequest request) {
        log.info("Updating supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> {
                log.debug("Supplier not found for update with ID: {}", id);
                return new NotFoundEntityException("Supplier with id " + id + " is not found");
            });

        if(request.getName() != null && !supplier.getName().contains(request.getName())) {
            if(supplierRepository.existsByName(request.getName())){
                log.debug("Supplier update failed - duplicate name: {}", request.getName());
                throw new DuplicateEntityException("Supplier with name " + request.getName() + " already exists");
            }
        }

        supplierMapper.updateEntityFromDto(request, supplier);
        SupplierDetailResponse response = supplierMapper.createDtoDetailResponseFromSupplier(supplier);
        log.debug("Supplier response created: {}", response);
        return response;
    }
    
}
