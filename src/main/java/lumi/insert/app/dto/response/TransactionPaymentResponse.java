package lumi.insert.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object representing a transaction payment record")
public record TransactionPaymentResponse(
    
    @Schema(description = "Unique identifier of the payment", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id, 
    
    @Schema(description = "ID of the associated transaction", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID transactionId, 
    
    @Schema(description = "Total amount paid or refunded", example = "150000")
    BigDecimal totalPayment, 
    
    @Schema(description = "Source of the payment (e.g., Customer Name or Bank Account)", example = "John Doe")
    String paymentFrom, 
    
    @Schema(description = "Destination of the payment (e.g., Company Account or Cashier)", example = "Main Store Account")
    String paymentTo, 
    
    @Schema(description = "Flag indicating if this record is a refund", example = "false")
    Boolean isForRefund,

    @Schema(description = "Pictures url that represent and desribe the payments")
    List<String> pictureUrl,

    @Schema(description = "Timestamp when the payment was added to the transaction")
    LocalDateTime createdAt,

    @Schema(description = "Timestamp when the payment was last updated")
    LocalDateTime updatedAt
    
)  implements Identifiable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}