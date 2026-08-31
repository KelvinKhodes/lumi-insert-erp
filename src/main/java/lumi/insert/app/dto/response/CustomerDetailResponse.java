package lumi.insert.app.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@Schema(description = "Detailed profile of a customer including contact information and transaction history summary")
public record CustomerDetailResponse(
    
    @Schema(description = "Primary key of the customer record", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID id, 
    
    @Schema(description = "Full name of the customer", example = "Budi Santoso")
    String name, 
    
    @Schema(description = "Customer's email address for digital invoices", example = "budi.s@email.com")
    String email, 
    
    @Schema(description = "Primary phone or WhatsApp number", example = "081234567890")
    String contact, 
    
    @Schema(description = "Default delivery or billing address", example = "Jl. Batuaji No. 12, Batam")
    String shippingAddress, 
    
    @Schema(description = "Lifetime count of purchases made by this customer", example = "25")
    Long totalTransaction, 
    
    @Schema(description = "Total outstanding debt/unpaid balance from this customer", example = "150000")
    BigDecimal totalUnpaid, 
    
    @Schema(description = "Total lifetime amount paid by this customer", example = "5250000")
    BigDecimal totalPaid,

    @Schema(description = "Remaining balance eligible for refund", example = "95000")
    BigDecimal totalUnrefunded,

    @Schema(description = "Total amount already refunded to the customer", example = "0")
    BigDecimal totalRefunded,

    @Schema(description = "Status indicating if the customer account is active", example = "true")
    Boolean isActive,

    @Schema(description = "Pictures url that represent and desribe the customer")
    List<String> pictureUrl

    )  implements Identifiable, Serializable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}