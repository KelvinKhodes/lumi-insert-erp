package lumi.insert.app.dto.response;

import java.io.Serializable;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard customer response containing basic contact information")
public record CustomerResponse(
    
    @Schema(description = "Primary key of the customer", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID id, 
    
    @Schema(description = "Full name of the customer", example = "Budi Santoso")
    String name, 
    
    @Schema(description = "Customer's active email address", example = "budi.s@email.com")
    String email, 
    
    @Schema(description = "Primary contact or WhatsApp number", example = "081234567890")
    String contact
    
)  implements Identifiable, Serializable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}