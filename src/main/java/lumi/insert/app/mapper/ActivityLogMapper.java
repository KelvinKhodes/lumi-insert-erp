package lumi.insert.app.mapper;

import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.dto.response.ActivityLogResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ActivityLogMapper {


  @Mapping(target = "ipAddress", constant = "*.*.*.* (Demo access)")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
  ActivityLogResponse createResponseFromEntity(ActivityLog activityLog);

}
