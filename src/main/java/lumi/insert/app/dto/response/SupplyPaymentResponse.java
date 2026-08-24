package lumi.insert.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object representing a payment record made to a supplier")
public record SupplyPaymentResponse(
    
    @Schema(description = "Unique identifier of the supply payment record", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id, 
    
    @Schema(description = "ID of the associated supply/procurement record", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID supplyId, 
    
    @Schema(description = "Total amount paid to the supplier or refunded from them", example = "2500000")
    BigDecimal totalPayment, 
    
    @Schema(description = "Source account or entity making the payment (e.g., Company Petty Cash)", example = "Main Office Cash")
    String paymentFrom, 
    
    @Schema(description = "Destination supplier account or representative", example = "Vendor Bank Account - ABC Corp")
    String paymentTo, 
    
    @Schema(description = "Flag indicating if this payment is a refund from the supplier (e.g., for returned goods)", example = "false")
    Boolean isForRefund,

    @Schema(description = "Timestamp when the PAYMENT was added to the supply")
    LocalDateTime createdAt,

    @Schema(description = "Timestamp when the PAYMENT was last updated")
    LocalDateTime updatedAt
    
)  implements Identifiable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}