package lumi.insert.app.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Detailed response representing a product in the inventory catalog")
public record ProductResponse (
    
    @Schema(description = "Unique identifier of the product", example = "1001")
    Long id, 
    
    @Schema(description = "Full name of the product", example = "Yamaha R25 Oil Filter")
    String name, 
    
    @Schema(description = "The cost price (Capital) paid to the supplier", example = "45000")
    BigDecimal basePrice, 
    
    @Schema(description = "The price at which the product is sold to customers", example = "65000")
    BigDecimal sellPrice, 
    
    @Schema(description = "Current available quantity in the warehouse", example = "150")
    BigDecimal stockQuantity, 
    
    @Schema(description = "The threshold level to trigger a restock alert", example = "10")
    BigDecimal stockMinimum, 
    
    @Schema(description = "Simplified category information this product belongs to")
    CategorySimpleResponse category,

    @Schema(description = "Active/archive status of the product")
    Boolean isActive,

    @Schema(description = "Pictures url that represent and desribe the product")
    List<String> pictureUrl,

    @Schema(description = "Timestamp when the product was first added to the system")
    LocalDateTime createdAt, 
    
    @Schema(description = "Timestamp of the last update to product details or stock")
    LocalDateTime updatedAt
    
)  implements Identifiable, Serializable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}