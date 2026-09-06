package lumi.insert.app.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lumi.insert.app.core.entity.nondatabase.EmployeeRole;

@Schema(description = "Response containing basic employee profile and system authorization role")
public record EmployeeResponse (
    
    @Schema(description = "Primary key of the employee", example = "b2c1d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e")
    UUID id, 
    
    @Schema(description = "Unique username used for system login", example = "budi_admin")
    String username, 
    
    @Schema(description = "Full legal name of the employee", example = "Budi Santoso")
    String fullname, 
    
    @Schema(description = "The authorization level or department assigned to this employee", example = "ADMIN")
    EmployeeRole role,

    @Schema(description = "Pictures url that represent and desribe the employee")
    String pictureUrl,

    @Schema(description = "Join date of the employee", example = "2024-12-31T23:59:59")
    LocalDateTime joinDate

) implements Identifiable {

    @Override
    public String getId() {
        return String.valueOf(this.id);
    }

}