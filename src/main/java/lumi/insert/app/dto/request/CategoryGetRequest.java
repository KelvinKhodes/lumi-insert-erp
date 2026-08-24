package lumi.insert.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter request for searching categories")
public class CategoryGetRequest extends PaginationRequest{

    @Builder.Default
    Boolean isArchived = false;

}
