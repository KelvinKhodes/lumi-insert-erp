package lumi.insert.app.utils.generator;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.dto.request.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import lumi.insert.app.core.entity.Customer;
import lumi.insert.app.core.entity.Product;
import lumi.insert.app.core.entity.StockCard;
import lumi.insert.app.core.entity.Supplier;
import lumi.insert.app.core.entity.Supply;
import lumi.insert.app.core.entity.SupplyPayment;
import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.TransactionPayment;

/**
 * Entities JpaSpecification Generator.
 * <p>Return Specification<any Entity> for Search Filter Query</p>
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Component
public class JpaSpecGenerator {
    
    public Pageable pageable(PaginationRequest request){
        Sort sort = Sort.by(request.getSortBy());

        if(request.getSortDirection().equalsIgnoreCase("DESC")){
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }

        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    public Specification<Customer> customerSpecification(CustomerGetByFilter request){
        Specification<Customer> specifications = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();

            if(request.getName() != null){
                predicates.add(builder.like(builder.lower(root.get("name")), "%" + request.getName().toLowerCase() + "%"));
            }
            if(request.getContact() != null){
                predicates.add(builder.equal(root.get("contact"), request.getContact()));
            }
            if(request.getEmail() != null){
                predicates.add(builder.equal(root.get("email"), request.getEmail()));
            }
            if(request.getIsActive() != null){
                predicates.add(builder.equal(root.get("isActive"), request.getIsActive()));
            }

            predicates.add(builder.between(root.get("totalTransaction"), request.getMinTotalTransaction(), request.getMaxTotalTransaction()));
            predicates.add(builder.between(root.get("totalUnpaid"), request.getMinTotalUnpaid(), request.getMaxTotalUnpaid()));
            predicates.add(builder.between(root.get("totalPaid"), request.getMinTotalPaid(), request.getMaxTotalPaid())); 
            return builder.and(predicates);
        };
        
        return specifications;
    }

    public Specification<Product> productSpecification(ProductGetByFilter request){
        Specification<Product> specification = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();

            if(request.getCategoryId() != null){
                predicates.add(builder.equal(root.get("category").get("id"), request.getCategoryId()));
            }
            if (request.getName() != null) {
                predicates.add(builder.like(builder.lower(root.get("name")), "%" + request.getName().toLowerCase() + "%"));
            }
            predicates.add(builder.isTrue(root.get("isActive")));
            predicates.add(builder.between(root.get("sellPrice"), request.getMinPrice(), request.getMaxPrice()));

            return builder.and(predicates);
        }; 
        return specification;
    }

    public Specification<Transaction> transactionSpecification(TransactionGetByFilter request){
        Specification<Transaction> specification = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();

            if(request.getStatus() != null){
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            if(request.getCustomerId() != null){
                predicates.add(builder.equal(root.get("customer").get("id"), request.getCustomerId()));
            }
            if (request.getMinCreatedAt() != null && request.getMaxCreatedAt() != null) {
                predicates.add(builder.between(root.get("createdAt"), request.getMinCreatedAt(), request.getMaxCreatedAt()));
            } 
            predicates.add(builder.between(root.get("totalItems"), request.getMinTotalItems(), request.getMaxTotalItems()));
            predicates.add(builder.between(root.get("grandTotal"), request.getMinGrandTotal(), request.getMaxGrandTotal()));
            predicates.add(builder.between(root.get("totalUnpaid"), request.getMinTotalUnpaid(), request.getMaxTotalUnpaid()));
            predicates.add(builder.between(root.get("totalPaid"), request.getMinTotalPaid(), request.getMaxTotalPaid()));

            return builder.and(predicates);
        }; 
        return specification;
    }

    public Specification<TransactionPayment> transactionPaymentSpecification(TransactionPaymentGetByFilter request){
        Specification<TransactionPayment> specification = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(request.getTransactionId() != null) predicates.add(builder.equal(root.get("transaction").get("id"), request.getTransactionId()));
            if(request.getMinCreatedAt() != null && request.getMaxCreatedAt() != null) predicates.add(builder.between(root.get("createdAt"), request.getMinCreatedAt(), request.getMaxCreatedAt()));

            predicates.add(builder.between(root.get("totalPayment"), request.getMinTotalPayment(), request.getMaxTotalPayment()));

            return builder.and(predicates);
        };
        return specification;
    }

    public Specification<StockCard> stockCardSpecification(StockCardGetByFilter request){
        Specification<StockCard> specification = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(request.getMinCreatedAt() != null && request.getMaxCreatedAt() != null) predicates.add(builder.between(root.get("createdAt"), request.getMinCreatedAt(), request.getMaxCreatedAt()));
            if(request.getProductId() != null) predicates.add(builder.equal(root.get("product").get("id"), request.getProductId()));
            if(request.getType() != null) predicates.add(builder.equal(root.get("type"), request.getType()));
            if(request.getLastId() != null) predicates.add(builder.greaterThan(root.get("id"), request.getLastId())); 

            return builder.and(predicates);
        };
        return specification;
    }

    public Specification<Supplier> supplierSpecification(SupplierGetByFilter request){
        Specification<Supplier> specifications = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();

            if(request.getName() != null){
                predicates.add(builder.equal(root.get("name"), request.getName()));
            }
            if(request.getContact() != null){
                predicates.add(builder.equal(root.get("contact"), request.getContact()));
            }
            if(request.getEmail() != null){
                predicates.add(builder.equal(root.get("email"), request.getEmail()));
            }
            if(request.getIsActive() != null){
                predicates.add(builder.equal(root.get("isActive"), request.getIsActive()));
            }

            predicates.add(builder.between(root.get("totalTransaction"), request.getMinTotalTransaction(), request.getMaxTotalTransaction()));
            predicates.add(builder.between(root.get("totalUnpaid"), request.getMinTotalUnpaid(), request.getMaxTotalUnpaid()));
            predicates.add(builder.between(root.get("totalPaid"), request.getMinTotalPaid(), request.getMaxTotalPaid())); 
            return builder.and(predicates);
        };
        
        return specifications;
    }

    public Specification<Supply> supplySpecification(SupplyGetByFilter request){
        Specification<Supply> specification = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();

            if(request.getStatus() != null){
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            if(request.getSupplierId() != null){
                predicates.add(builder.equal(root.get("supplier").get("id"), request.getSupplierId()));
            }
            if (request.getMinCreatedAt() != null && request.getMaxCreatedAt() != null) {
                predicates.add(builder.between(root.get("createdAt"), request.getMinCreatedAt(), request.getMaxCreatedAt()));
            } 
            predicates.add(builder.between(root.get("totalItems"), request.getMinTotalItems(), request.getMaxTotalItems()));
            predicates.add(builder.between(root.get("grandTotal"), request.getMinGrandTotal(), request.getMaxGrandTotal()));
            predicates.add(builder.between(root.get("totalUnpaid"), request.getMinTotalUnpaid(), request.getMaxTotalUnpaid()));
            predicates.add(builder.between(root.get("totalPaid"), request.getMinTotalPaid(), request.getMaxTotalPaid()));

            return builder.and(predicates);
        }; 
        return specification;
    }

    public Specification<SupplyPayment> supplyPaymentSpecification(SupplyPaymentGetByFilter request){
        Specification<SupplyPayment> specification = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(request.getSupplyId() != null) predicates.add(builder.equal(root.get("supply").get("id"), request.getSupplyId()));
            if(request.getMinCreatedAt() != null && request.getMaxCreatedAt() != null) predicates.add(builder.between(root.get("createdAt"), request.getMinCreatedAt(), request.getMaxCreatedAt()));

            predicates.add(builder.between(root.get("totalPayment"), request.getMinTotalPayment(), request.getMaxTotalPayment()));

            return builder.and(predicates);
        };
        return specification;
    }

    public Specification<ActivityLog> activityLogSpecification(ActivityLogFilterRequest request){
        Specification<ActivityLog> specification = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(request.getEntityName() != null){
                predicates.add(builder.equal(root.get("entityName"), request.getEntityName()));
            }
            if(request.getEntityId() != null){
                predicates.add(builder.equal(root.get("entityId"), request.getEntityId()));
            }
            if(request.getAction() != null){
                predicates.add(builder.equal(root.get("action"), request.getAction()));
            }
            if(request.getCreatedBy() != null){
                predicates.add(builder.equal(root.get("createdBy"), request.getCreatedBy()));
            }
            if(request.getIpAddress() != null){
                predicates.add(builder.equal(root.get("ipAddress"), request.getIpAddress()));
            }
            if(request.getMinCreatedAt() != null && request.getMaxCreatedAt() != null) predicates.add(builder.between(root.get("createdAt"), request.getMinCreatedAt(), request.getMaxCreatedAt()));
            return builder.and(predicates);
        };

        return specification;
    }
}
