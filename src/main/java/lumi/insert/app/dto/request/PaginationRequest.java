package lumi.insert.app.dto.request;

import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pagination parameters for list requests")
public class PaginationRequest {
    
    @Builder.Default
    @Schema(description = "Page number (0-based)", example = "0")
    private Integer page = 0;

    @Builder.Default
    @Schema(description = "Page size/number of records per page", example = "10")
    @Max(value = 100)
    private Integer size = 10;

    @Builder.Default
    @Schema(description = "Field name to sort by", example = "createdAt")
    String sortBy = "createdAt";

    @Builder.Default
    @Schema(description = "Sort direction (ASC or DESC)", example = "DESC")
    String sortDirection ="DESC";
    
}
