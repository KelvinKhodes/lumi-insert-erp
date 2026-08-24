package lumi.insert.app.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.TransactionItem;
import lumi.insert.app.core.entity.TransactionPayment;
import lumi.insert.app.dto.response.TransactionDetailResponse;
import lumi.insert.app.dto.response.TransactionItemDelete; 
import lumi.insert.app.dto.response.TransactionItemResponse;
import lumi.insert.app.dto.response.TransactionPaymentResponse;
import lumi.insert.app.dto.response.TransactionResponse;

/**
 * Mapper for {@link Transaction}, mapped from DTO to Entity and Otherwise.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Mapper(componentModel = "spring", uses = ProductMapper.class)
public interface AllTransactionMapper {
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "customerId", source = "transaction.customer.id")
    @Mapping(target = "messages", source = "messages")
    TransactionResponse createTransactionResponseDto(Transaction transaction, List<String> messages);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "customerId", source = "customer.id")
    TransactionResponse createTransactionResponseDto(Transaction transaction);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "customerId", source = "customer.id")
    TransactionDetailResponse createTransactionDetailResponseDto(Transaction transaction);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "transactionId", source = "transaction.id")
    TransactionItemResponse createTransactionItemResponseDto(TransactionItem transactionItem);
  
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "deleted", constant = "true")
    TransactionItemDelete createTransactionItemDeleteResponseDto(TransactionItem transactionItem);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "transactionId", source = "transaction.id")
    TransactionPaymentResponse createTransactionPaymentResponseDto(TransactionPayment transactionPayment);
}
