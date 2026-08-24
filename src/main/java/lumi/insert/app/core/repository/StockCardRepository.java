package lumi.insert.app.core.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import lumi.insert.app.core.entity.StockCard;
import lumi.insert.app.dto.response.StockCardResponse; 

/**
 * Repository for {@link StockCard} entity.
 * <p>Support {@link JpaSpecificationExecutor} query.</p>
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Repository
public interface StockCardRepository extends JpaRepository<StockCard, UUID>, JpaSpecificationExecutor<StockCard>{
    
    @Query("SELECT s.id as id, s.referenceId as referenceId, s.product.id as productId, s.productName as productName, s.quantity as quantity, s.oldStock as oldStock, s.newStock as newStock, s.oldPrice as oldPrice, s.newPrice as newPrice, s.type as type, s.description as description, s.createdAt as createdAt " + 
        "FROM stock_cards s WHERE " +
        "s.createdAt between :minTime and :maxTime " +
        "AND :lastId IS NULL OR s.id > :lastId " +
        "ORDER BY s.createdAt ASC")
    Slice<StockCardResponse> findByIndexPagination(@Param("minTime") LocalDateTime minTime,
                                        @Param("maxTime") LocalDateTime maxTime, 
                                        @Param("lastId") UUID lastId, 
                                        Pageable pageable);

    Slice<StockCard> findAllByReferenceId(UUID refId);

    @Query(
        "SELECT s FROM stock_cards s " +
        "WHERE s.product.id = :productId AND s.type = lumi.insert.app.core.entity.nondatabase.StockMove.PURCHASE " +
        "ORDER BY s.createdAt DESC LIMIT 1"
    )
    Optional<StockCard> getLastPurchase(Long productId);
 
}
