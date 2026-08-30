package lumi.insert.app.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Schema(description = "Filter request for searching transaction payments")
public class TransactionPaymentGetByFilter extends PaginationRequest{
 
    @Schema(description = "Filter by transaction ID", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID transactionId;

    @Builder.Default
    @Min(value = 0, message = "minTotalPayment minimal value is 0")
    @Schema(description = "Minimum payment amount", example = "50000")
    BigDecimal minTotalPayment = BigDecimal.ZERO;

    @Builder.Default
    @Min(value = 0, message = "maxTotalPayment minimal value is 0")
    @Schema(description = "Maximum payment amount", example = "5000000")
    BigDecimal maxTotalPayment = BigDecimal.valueOf(9999999999990L);

    @Schema(description = "Start date for payment range", example = "2024-01-01T00:00:00")
    LocalDateTime minCreatedAt;

    @Schema(description = "End date for payment range", example = "2024-12-31T23:59:59")
    LocalDateTime maxCreatedAt;

    @Builder.Default
    @Pattern(regexp = "createdAt|updatedAt|totalPayment", message = "check documentation for sortBy specification")
    String sortBy = "createdAt";

    @Builder.Default
    @Pattern(regexp = "DESC|ASC", message = "check documentation for sortDirection specification")
    String sortDirection = "DESC";

    /**
     * Raw Condition
     * condition =
     *             "&& (#request.getPage == 0 && #request.getSize == 10) " +
     *             "&& (#request.name == null || #request.name.isEmpty()) && (#request.email == null || #request.email.isEmpty()) " +
     *             "&& (#request.contact == null || #request.contact.isEmpty()) && #request.isActive == null " +
     *             "&& #request.minTotalTransaction.compareTo(T(java.math.BigDecimal).ZERO) == 0 " +
     *             "&& #request.maxTotalTransaction.compareTo(T(java.math.BigDecimal).valueOf(9999990L)) == 0 " +
     *             "&& #request.minTotalUnpaid.compareTo(T(java.math.BigDecimal).ZERO) == 0 " +
     *             "&& #request.maxTotalUnpaid.compareTo(T(java.math.BigDecimal).valueOf(9999999999990L)) == 0 " +
     *             "&& #request.minTotalPaid.compareTo(T(java.math.BigDecimal).ZERO) == 0 " +
     *             "&& #request.maxTotalPaid.compareTo(T(java.math.BigDecimal).valueOf(9999999999990L)) == 0"
     * @return boolean of condition
     */
    public boolean isForDashboard(){
        return (this.getPage() == 0 && this.getSize() == 10
            && this.transactionId == null
            && this.minTotalPayment.compareTo(BigDecimal.ZERO) == 0
            && this.maxTotalPayment.compareTo(BigDecimal.valueOf(9999999999990L)) == 0
            && this.minCreatedAt == null
            && this.maxCreatedAt== null);
    }
}
