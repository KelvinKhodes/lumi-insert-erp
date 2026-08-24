package lumi.insert.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductStatisticGetRequest {
       
    @Schema(description = "Start date for statistics range", example = "2024-01-01T00:00:00")
    private LocalDateTime startDate;
 
    @Schema(description = "End date for statistics range", example = "2024-01-01T00:00:00")
    private LocalDateTime endDate; 

}
