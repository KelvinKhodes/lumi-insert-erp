package lumi.insert.app.core.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lumi.insert.app.core.entity.nondatabase.BaseAuditing;

/**
 * Representation class of database table {@code"customers"}. 
 * <p>Store credential informations such as token, Employee and token expired date.<br>
 * Class can be implemented by{@code NoArgsConstructor, AllArgsConstructor and Builder}. </p>
 * 
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Entity(name = "customers")
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Audited
public class Customer extends BaseAuditing{
    
    /**
     * Unique identifier of entity .
     * <p>Strongly recommend to assign manually use {@link UuidCreator#getTimeOrderedEpochFast()}.</p>
     */
    @Id 
    private UUID id;

    /**
     * Customer name. 
     ** <p>Important: must not null and unique.</p> 
     */
    @Column(unique = true, nullable = false )
    private String name;
 
    /**
     * Customer email. 
     ** <p>All email services(Invoice order, Etc) will send to this value if not null</p> 
     */
    private String email;

    /**
     * Customer contant. 
     ** <p>Important: must not null.</p> 
     */
    @Column(nullable = false )
    private String contact;

    /**
     * Customer shipping address. 
     ** <p>Important: must not null.</p> 
     */
    @Column(nullable = false )
    private String shippingAddress; 

    private Double Latitude; 

    private Double Longitude;

    @Builder.Default 
    private Boolean isActive = true;

    
    /**
     * Total transactions related, count by increment each product saved. 
     ** <p>Note: Default value is 0</p> 
     */
    @Builder.Default 
    @NotAudited
    private Long totalTransaction = 0L;

    @Builder.Default
    @NotAudited
    private BigDecimal totalUnpaid = BigDecimal.ZERO;

    @Builder.Default
    @NotAudited
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Builder.Default
    @NotAudited
    private BigDecimal totalUnrefunded = BigDecimal.ZERO;

    @Builder.Default
    @NotAudited
    private BigDecimal totalRefunded = BigDecimal.ZERO;

    @OneToMany(mappedBy = "customer")
    @Builder.Default
    @ToString.Exclude
    @NotAudited
    private List<Transaction> transactions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @NotAudited 
    @Builder.Default
    private List<String> pictureUrl = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    @Builder.Default
    @ToString.Exclude
    @NotAudited
    private List<CustomerPicture> customerPictures = new ArrayList<>();

    /**
     * Function to increment {@link #totalTransaction}
     */
    public void addTransaction(){
        this.totalTransaction ++;
    }

    /**
     * Pre-query function used to normalize BigDecimal scale
     * <p>Rounding half up  and scaled by 4 for precise value</p>
     */
    @PrePersist
    @PreUpdate
    void normalizeDecimal(){
        this.totalUnpaid = this.totalUnpaid.setScale(4, RoundingMode.HALF_UP);
        this.totalPaid = this.totalPaid.setScale(4, RoundingMode.HALF_UP);
        this.totalUnrefunded = this.totalUnrefunded.setScale(4, RoundingMode.HALF_UP);
        this.totalRefunded = this.totalRefunded.setScale(4, RoundingMode.HALF_UP);
    }
}
