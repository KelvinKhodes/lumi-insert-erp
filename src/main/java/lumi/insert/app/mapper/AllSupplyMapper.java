package lumi.insert.app.mapper;
 

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import lumi.insert.app.core.entity.Supply;
import lumi.insert.app.core.entity.SupplyItem;
import lumi.insert.app.core.entity.SupplyPayment;
import lumi.insert.app.dto.request.SupplyUpdateRequest;
import lumi.insert.app.dto.response.SupplyDetailResponse;
import lumi.insert.app.dto.response.SupplyItemResponse;
import lumi.insert.app.dto.response.SupplyPaymentResponse;
import lumi.insert.app.dto.response.SupplyResponse;  

/**
 * Mapper for {@link Supply}, mapped from DTO to Entity and Otherwise.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Mapper(componentModel = "spring", uses = ProductMapper.class)
public interface AllSupplyMapper {
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "supplierId", source = "supplier.id")
    SupplyResponse createSimpleDTO(Supply supply);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "supplierId", source = "supplier.id")
    SupplyDetailResponse createDetailDTO(Supply supply);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE) 
    SupplyItemResponse createItemResponseDTO(SupplyItem supplyItem);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE) 
    void updateSupply(SupplyUpdateRequest request, @MappingTarget Supply supply); 

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "supplyId", source = "supply.id")
    SupplyPaymentResponse createSupplyPaymentResponseDto(SupplyPayment supplyPayment);
}
