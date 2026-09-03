package lumi.insert.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Schema(description = "Request to create a new product category")
public class ActivityLogFilterRequest extends PaginationRequest{

    @Schema(description = "Filter by entity name", example = "products")
    String entityName;

    @Schema(description = "Filter by entity Id", example = "10 < Product ID or f47ac10b-58cc-4372-a567-0e02b2c3d479 for UUID")
    String entityId;

    @Schema(description = "Filter by action", example = "PRODUCT_CREATED")
    ActivityAction action;

    @Schema(description = "The requester username", example = "OWNER")
    String createdBy;

    @Schema(description = "Start date range of activity log", example = "2024-01-01T00:00:00")
    LocalDateTime minCreatedAt;

    @Schema(description = "End date range of activity log", example = "2024-12-31T23:59:59")
    LocalDateTime maxCreatedAt;

    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
    @Schema(description = "IP Address of the requester", example = "0.0.1.1")
    String ipAddress;
}
