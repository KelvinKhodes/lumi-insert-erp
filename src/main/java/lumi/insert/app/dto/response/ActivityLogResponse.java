package lumi.insert.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lumi.insert.app.core.entity.nondatabase.ActivityAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Response object representing an detailed activity log")
public record ActivityLogResponse(

    @Schema(description = "Unique identifier of the activity log", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    UUID id,

    @Schema(description = "Name of entity that involved the action", example = "products")
    String entityName,

    @Schema(description = "Identifier of the entity", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    String entityId,

    @Schema(description = "Represent the action from activity", example = "PRODUCT_CREATED")
    ActivityAction action,

    @Schema(description = "additional message", example = "Product created")
    String actionMessage,

    @Schema(description = "The requester")
    String createdBy,

    @Schema(description = "Timestamp when the action complete")
    LocalDateTime createdAt,

    @Schema(description = "IP address of the requester")
    String ipAddress
) {
}
