package lumi.insert.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter request for searching customers")
public class CustomerGetByFilter extends PaginationRequest{

    @Pattern(regexp = "^[a-zA-Z0-9.-_ ]{5,30}$", message = "name has to be 5-30 length")
    @Schema(description = "Filter customer by name", example = "John Wholesale")
    String name;

    @Email(message = "email doesn't meet the email format")
    @Schema(description = "Filter customer by email", example = "john@example.com")
    String email;

    @Schema(description = "Filter customer by phone contact", example = "081234567890")
    String contact;

    @Schema(description = "Filter customers by active status", example = "true")
    Boolean isActive;

    @Builder.Default
    @Min(value = 0, message = "minTotalItems minimal value is 0")
    @Schema(description = "Minimum number of customer transactions", example = "0")
    Long minTotalTransaction = 0L;

    @Builder.Default
    @Min(value = 0, message = "maxTotalItems minimal value is 0")
    @Schema(description = "Maximum number of customer transactions", example = "100")
    Long maxTotalTransaction = 9999990L;

    @Builder.Default
    @Min(value = 0, message = "minGrandTotal minimal value is 0")
    @Schema(description = "Minimum unpaid amount from customer", example = "0")
    BigDecimal minTotalUnpaid = BigDecimal.ZERO;

    @Builder.Default
    @Min(value = 0, message = "maxGrandTotal minimal value is 0")
    @Schema(description = "Maximum unpaid amount from customer", example = "10000000")
    BigDecimal maxTotalUnpaid = BigDecimal.valueOf(9999999999990L);

    @Builder.Default
    @Min(value = 0, message = "minTotalUnpaid minimal value is 0")
    @Schema(description = "Minimum paid amount from customer", example = "0")
    BigDecimal minTotalPaid = BigDecimal.ZERO;

    @Builder.Default
    @Min(value = 0, message = "maxTotalUnpaid minimal value is 0")
    @Schema(description = "Maximum paid amount from customer", example = "50000000")
    BigDecimal maxTotalPaid = BigDecimal.valueOf(9999999999990L);

    @Builder.Default
    @Pattern(regexp = "createdAt|updatedAt|totalTransaction|totalUnpaid|totalPaid|name", message = "check documentation for sortBy specification")
    String sortBy = "createdAt";

    @Builder.Default
    @Pattern(regexp = "DESC|ASC", message = "check documentation for sortDirection specification")
    String sortDirection = "DESC";

    /**
     * Raw Condition
     * condition =
     *             "&& (#request.getPage == 0 && #request.getSize == 12) " +
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
    public boolean isForFirstPage(){
        return (this.getPage() == 0 && this.getSize() == 12  && (this.name == null || this.name.isEmpty())
            && (this.email == null || this.email.isEmpty())
            && (this.contact == null || this.contact.isEmpty()) && this.isActive == null 
            && this.minTotalTransaction == 0L
            && this.maxTotalTransaction == 9999990L
            && this.minTotalUnpaid.compareTo(BigDecimal.ZERO) == 0 
            && this.maxTotalUnpaid.compareTo(BigDecimal.valueOf(9999999999990L)) == 0 
            && this.minTotalPaid.compareTo(BigDecimal.ZERO) == 0
            && this.maxTotalPaid.compareTo(BigDecimal.valueOf(9999999999990L)) == 0);
    }
}
