package lumi.insert.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight response containing only the basic identity of a product category")
public record CategorySimpleResponse(
    
    @Schema(description = "Unique identifier of the category", example = "10")
    Long id, 
    
    @Schema(description = "Display name of the category", example = "Onderdil Motor")
    String name,

    @Schema(description = "Active/archive status of the Category")
    Boolean isActive

) implements Identifiable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}