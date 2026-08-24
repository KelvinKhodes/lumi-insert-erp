package lumi.insert.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "Minimalist response containing only the identity and name of a product")
public record ProductName (
    
    @Schema(description = "Primary key of the product", example = "1001")
    Long id, 
    
    @Schema(description = "Full display name of the product", example = "Oli Yamalube Super Sport 1L")
    String name,

    @Schema(description = "The current number of units available in the warehouse/store", example = "42")
    BigDecimal stockQuantity
    
)  implements Identifiable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}