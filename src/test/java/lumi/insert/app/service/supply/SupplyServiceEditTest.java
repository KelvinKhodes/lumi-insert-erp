package lumi.insert.app.service.supply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.f4b6a3.uuid.UuidCreator;
import lumi.insert.app.core.entity.Supply;
import lumi.insert.app.core.entity.SupplyItem;
import lumi.insert.app.core.entity.nondatabase.SupplyStatus;
import lumi.insert.app.dto.request.ItemRefundRequest;
import lumi.insert.app.dto.request.SupplyUpdateRequest;
import lumi.insert.app.dto.response.SupplyResponse;
import lumi.insert.app.exception.ForbiddenRequestException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.TransactionValidationException;

public class SupplyServiceEditTest extends BaseSupplyServiceTest {

    @Test
    @DisplayName("Should throw ForbiddenRequestException when trx status isn't pending > setToProcess only from pending")
    public void cancelSupply_statusNonPending_throwNotFound() {
        setupSupply.setStatus(SupplyStatus.CANCELLED);
        when(supplyRepositoryMock.findByIdDetail(any(UUID.class))).thenReturn(Optional.of(setupSupply));

        assertThrows(ForbiddenRequestException.class, () -> supplyServiceMock.cancelSupply(setupSupply.getId()));
    }

    @Test
    @DisplayName("Should throw NotFoundEntityException when id is invalid for Complete")
    public void cancelSupply_invalidId_throwNotFound() {
        when(supplyRepositoryMock.findByIdDetail(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> supplyServiceMock.cancelSupply(setupSupply.getId()));
    }

    @Test
    @DisplayName("Should cancel and add product stock when cancelTrx complete")
    public void cancelSupply_validRequest_returnDtoAndReverseProduct() {
        setupSupplier.setTotalPaid(BigDecimal.valueOf(230L));
        setupSupplier.setTotalUnpaid(BigDecimal.valueOf(3000L));
        setupSupplier.setTotalUnrefunded(BigDecimal.valueOf(9L));

        setupProduct.setStockQuantity(BigDecimal.valueOf(7L));
        setupProduct.setSellPrice(BigDecimal.valueOf(1000L));
        setupProduct.setBasePrice(BigDecimal.valueOf(800L));

        setupSupplyItem.setProduct(setupProduct);
        setupSupplyItem.setQuantity(BigDecimal.valueOf(4L));
        setupSupplyItem.setPrice(BigDecimal.valueOf(820L));

        setupSupply.setTotalUnpaid(BigDecimal.valueOf(3000L));
        setupSupply.setTotalPaid(BigDecimal.valueOf(200L));
        setupSupply.getSupplyItems().add(setupSupplyItem);
        setupSupply.setSupplier(setupSupplier);
        setupSupply.setStatus(SupplyStatus.UNPAID);

        setupStockCard.setNewStock(setupProduct.getStockQuantity());
        setupStockCard.setNewPrice(setupProduct.getBasePrice());

        when(supplyRepositoryMock.findByIdDetail(any())).thenReturn(Optional.of(setupSupply));
        when(stockCardRepositoryMock.getLastPurchase(setupProduct.getId())).thenReturn(Optional.of(setupStockCard));

        SupplyResponse cancelSupply = supplyServiceMock.cancelSupply(UUID.randomUUID());
        assertEquals(SupplyStatus.CANCELLED, cancelSupply.status());
        assertTrue(BigDecimal.valueOf(0L).compareTo(cancelSupply.totalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(cancelSupply.totalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(200L).compareTo(cancelSupply.totalUnrefunded()) == 0);

        assertTrue(BigDecimal.valueOf(30L).compareTo(setupSupplier.getTotalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(setupSupplier.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(209L).compareTo(setupSupplier.getTotalUnrefunded()) == 0);

        assertTrue(new BigDecimal("773.3333").compareTo(setupProduct.getBasePrice()) == 0);
        assertTrue(BigDecimal.valueOf(3L).compareTo(setupProduct.getStockQuantity()) == 0);
    }

    @Test
    @DisplayName("Should cancel and add product stock when cancelTrx complete")
    public void cancelSupply_validRequest2_returnDtoAndReverseProduct() {
        setupSupplier.setTotalPaid(BigDecimal.valueOf(230L));
        setupSupplier.setTotalUnpaid(BigDecimal.valueOf(1400L));
        setupSupplier.setTotalUnrefunded(BigDecimal.valueOf(9L));

        setupProduct.setStockQuantity(BigDecimal.valueOf(2L));
        setupProduct.setSellPrice(BigDecimal.valueOf(1000L));
        setupProduct.setBasePrice(BigDecimal.valueOf(800L));

        setupSupplyItem.setProduct(setupProduct);
        setupSupplyItem.setQuantity(BigDecimal.valueOf(4L));
        setupSupplyItem.setPrice(BigDecimal.valueOf(820L));

        SupplyItem partialRefund = SupplyItem.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .product(setupProduct)
                .quantity(BigDecimal.valueOf(-2L))
                .price(BigDecimal.valueOf(820L))
                .build();

        setupSupply.setTotalUnpaid(BigDecimal.valueOf(1400L));
        setupSupply.setTotalUnrefunded(BigDecimal.valueOf(0L));
        setupSupply.setTotalPaid(BigDecimal.valueOf(200L));
        setupSupply.getSupplyItems().add(setupSupplyItem);
        setupSupply.getSupplyItems().add(partialRefund);
        setupSupply.setSupplier(setupSupplier);
        setupSupply.setStatus(SupplyStatus.UNPAID);

        when(supplyRepositoryMock.findByIdDetail(any())).thenReturn(Optional.of(setupSupply));

        SupplyResponse cancelSupply = supplyServiceMock.cancelSupply(UUID.randomUUID());
        assertEquals(SupplyStatus.CANCELLED, cancelSupply.status());
        assertTrue(BigDecimal.valueOf(0L).compareTo(cancelSupply.totalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(cancelSupply.totalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(200L).compareTo(cancelSupply.totalUnrefunded()) == 0);

        assertTrue(BigDecimal.valueOf(30L).compareTo(setupSupplier.getTotalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(setupSupplier.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(209L).compareTo(setupSupplier.getTotalUnrefunded()) == 0);

        assertTrue(BigDecimal.valueOf(800L).compareTo(setupProduct.getBasePrice()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(setupProduct.getStockQuantity()) == 0);
    }

    @Test
    @DisplayName("Should cancel and add product stock when cancelTrx complete")
    public void cancelSupply_validRequest3_returnDtoAndReverseProduct() {
        setupSupplier.setTotalPaid(BigDecimal.valueOf(30L));
        setupSupplier.setTotalUnpaid(BigDecimal.valueOf(0L));
        setupSupplier.setTotalUnrefunded(BigDecimal.valueOf(209L));

        setupProduct.setStockQuantity(BigDecimal.valueOf(0L));
        setupProduct.setSellPrice(BigDecimal.valueOf(1000L));
        setupProduct.setBasePrice(BigDecimal.valueOf(800L));

        setupSupplyItem.setProduct(setupProduct);
        setupSupplyItem.setQuantity(BigDecimal.valueOf(4L));
        setupSupplyItem.setPrice(BigDecimal.valueOf(820L));

        SupplyItem fullRefund = SupplyItem.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .product(setupProduct)
                .quantity(BigDecimal.valueOf(-4L))
                .price(BigDecimal.valueOf(820L))
                .build();

        setupSupply.setTotalUnpaid(BigDecimal.valueOf(0L));
        setupSupply.setTotalUnrefunded(BigDecimal.valueOf(200L));
        setupSupply.setTotalPaid(BigDecimal.valueOf(0L));
        setupSupply.getSupplyItems().add(setupSupplyItem);
        setupSupply.getSupplyItems().add(fullRefund);
        setupSupply.setSupplier(setupSupplier);
        setupSupply.setStatus(SupplyStatus.UNPAID);

        when(supplyRepositoryMock.findByIdDetail(any())).thenReturn(Optional.of(setupSupply));

        SupplyResponse cancelSupply = supplyServiceMock.cancelSupply(UUID.randomUUID());
        assertEquals(SupplyStatus.CANCELLED, cancelSupply.status());
        assertTrue(BigDecimal.valueOf(0L).compareTo(cancelSupply.totalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(cancelSupply.totalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(200L).compareTo(cancelSupply.totalUnrefunded()) == 0);

        assertTrue(BigDecimal.valueOf(30L).compareTo(setupSupplier.getTotalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(setupSupplier.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(209L).compareTo(setupSupplier.getTotalUnrefunded()) == 0);

        assertTrue(BigDecimal.valueOf(800L).compareTo(setupProduct.getBasePrice()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(setupProduct.getStockQuantity()) == 0);
    }

    @Test
    @DisplayName("Update Supply: Discount exceeds unpaid amount")
    public void updateSupply_extremeDiscount() {
        UUID id = UUID.randomUUID();

        setupSupplier.setTotalUnpaid(BigDecimal.valueOf(1000L));
        setupSupplier.setTotalPaid(BigDecimal.valueOf(500L));
        setupSupplier.setTotalUnrefunded(BigDecimal.valueOf(0L));

        Supply supply = Supply.builder()
                .subTotal(BigDecimal.valueOf(200L))
                .grandTotal(BigDecimal.valueOf(200L))
                .totalUnpaid(BigDecimal.valueOf(40L))
                .totalPaid(BigDecimal.valueOf(160L))
                .totalUnrefunded(BigDecimal.valueOf(0L))
                .supplier(setupSupplier)
                .build();

        SupplyUpdateRequest request = SupplyUpdateRequest.builder()
                .totalDiscount(BigDecimal.valueOf(50L))
                .totalFee(BigDecimal.valueOf(0L))
                .build();

        when(supplyRepositoryMock.findByIdDetail(id)).thenReturn(Optional.of(supply));

        SupplyResponse updateSupply = supplyServiceMock.updateSupply(id, request);

        assertTrue(BigDecimal.valueOf(150L).compareTo(updateSupply.grandTotal()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(updateSupply.totalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(150L).compareTo(updateSupply.totalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(10L).compareTo(updateSupply.totalUnrefunded()) == 0);

        assertTrue(BigDecimal.valueOf(150L).compareTo(supply.getGrandTotal()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(supply.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(150L).compareTo(supply.getTotalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(960L).compareTo(setupSupplier.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(490L).compareTo(setupSupplier.getTotalPaid()) == 0);
        assertTrue(BigDecimal.valueOf(10L).compareTo(setupSupplier.getTotalUnrefunded()) == 0);
    }

    @Test
    @DisplayName("Update Supply: Fee increases total cost")
    public void updateSupply_feeIncrease() {
        UUID id = UUID.randomUUID();

        setupSupplier.setTotalUnpaid(BigDecimal.valueOf(1000L));
        setupSupplier.setTotalPaid(BigDecimal.valueOf(500L));
        setupSupplier.setTotalUnrefunded(BigDecimal.valueOf(0L));

        Supply supply = Supply.builder()
                .subTotal(BigDecimal.valueOf(200L))
                .grandTotal(BigDecimal.valueOf(200L))
                .totalUnpaid(BigDecimal.valueOf(40L))
                .totalPaid(BigDecimal.valueOf(160L))
                .totalUnrefunded(BigDecimal.valueOf(0L))
                .supplier(setupSupplier)
                .build();

        SupplyUpdateRequest request = SupplyUpdateRequest.builder()
                .totalDiscount(BigDecimal.valueOf(0L))
                .totalFee(BigDecimal.valueOf(50L))
                .build();

        when(supplyRepositoryMock.findByIdDetail(id)).thenReturn(Optional.of(supply));

        SupplyResponse updateSupply = supplyServiceMock.updateSupply(id, request);

        assertTrue(BigDecimal.valueOf(250L).compareTo(updateSupply.grandTotal()) == 0);
        assertTrue(BigDecimal.valueOf(90L).compareTo(updateSupply.totalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(160L).compareTo(updateSupply.totalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(250L).compareTo(supply.getGrandTotal()) == 0);
        assertTrue(BigDecimal.valueOf(90L).compareTo(supply.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(160L).compareTo(supply.getTotalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(1050L).compareTo(setupSupplier.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(500L).compareTo(setupSupplier.getTotalPaid()) == 0);
    }

    @Test
    @DisplayName("Update Supply: Fee increases total cost")
    public void refundSupplyItem_validRequest_returnAndCalculate() {

        setupSupplier.setTotalUnpaid(BigDecimal.valueOf(1000L));
        setupSupplier.setTotalPaid(BigDecimal.valueOf(500L));
        setupSupplier.setTotalUnrefunded(BigDecimal.valueOf(0L));

        Supply supply = Supply.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .subTotal(BigDecimal.valueOf(1500L))
                .grandTotal(BigDecimal.valueOf(1500L))
                .totalUnpaid(BigDecimal.valueOf(1000L))
                .totalPaid(BigDecimal.valueOf(500L))
                .totalUnrefunded(BigDecimal.valueOf(0L))
                .supplier(setupSupplier)
                .build();

        setupProduct.setStockQuantity(BigDecimal.valueOf(3L));
        setupProduct.setBasePrice(BigDecimal.valueOf(510L));

        setupSupplyItem.setPrice(BigDecimal.valueOf(500L));
        setupSupplyItem.setQuantity(BigDecimal.valueOf(3L));
        setupSupplyItem.setProduct(setupProduct);
        setupSupplyItem.setSupply(supply);

        ItemRefundRequest request = ItemRefundRequest.builder()
                .productId(setupProduct.getId())
                .quantity(BigDecimal.valueOf(2L))
                .build();

        setupStockCard.setNewStock(setupProduct.getStockQuantity());
        setupStockCard.setNewPrice(setupProduct.getBasePrice());

        when(supplyItemRepositoryMock.findBySupplyIdAndProductId(supply.getId(), request.getProductId())).thenReturn(List.of(setupSupplyItem));
        when(stockCardRepositoryMock.getLastPurchase(setupProduct.getId())).thenReturn(Optional.of(setupStockCard));

        SupplyResponse updateSupply = supplyServiceMock.refundSupplyItem(supply.getId(), request);

        assertTrue(BigDecimal.valueOf(0L).compareTo(updateSupply.totalUnrefunded()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(updateSupply.totalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(500L).compareTo(updateSupply.totalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(0L).compareTo(supply.getTotalUnrefunded()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(supply.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(500L).compareTo(supply.getTotalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(0L).compareTo(setupSupplier.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(500L).compareTo(setupSupplier.getTotalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(530L).compareTo(setupProduct.getBasePrice()) == 0);
        assertTrue(BigDecimal.valueOf(1L).compareTo(setupProduct.getStockQuantity()) == 0);
    }

    @Test
    @DisplayName("Update Supply: Fee increases total cost")
    public void refundSupplyItem_validRequest2_returnAndCalculate() {

        setupSupplier.setTotalUnpaid(BigDecimal.valueOf(500L));
        setupSupplier.setTotalPaid(BigDecimal.valueOf(500L));
        setupSupplier.setTotalUnrefunded(BigDecimal.valueOf(0L));

        Supply supply = Supply.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .subTotal(BigDecimal.valueOf(1500L))
                .grandTotal(BigDecimal.valueOf(1500L))
                .totalUnpaid(BigDecimal.valueOf(500L))
                .totalPaid(BigDecimal.valueOf(500L))
                .totalUnrefunded(BigDecimal.valueOf(0L))
                .supplier(setupSupplier)
                .build();

        setupProduct.setStockQuantity(BigDecimal.valueOf(2L));
        setupProduct.setBasePrice(BigDecimal.valueOf(515L));

        setupSupplyItem.setPrice(BigDecimal.valueOf(500L));
        setupSupplyItem.setQuantity(BigDecimal.valueOf(3L));
        setupSupplyItem.setProduct(setupProduct);
        setupSupplyItem.setSupply(supply);

        SupplyItem refundItem = SupplyItem.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .price(BigDecimal.valueOf(500L))
                .quantity(BigDecimal.valueOf(-1L))
                .supply(supply)
                .product(setupProduct)
                .build();

        ItemRefundRequest request = ItemRefundRequest.builder()
                .productId(setupProduct.getId())
                .quantity(BigDecimal.valueOf(1L))
                .build();

        setupStockCard.setNewStock(setupProduct.getStockQuantity().add(BigDecimal.valueOf(1)));
        setupStockCard.setNewPrice(BigDecimal.valueOf(510));

        when(supplyItemRepositoryMock.findBySupplyIdAndProductId(supply.getId(), request.getProductId())).thenReturn(List.of(setupSupplyItem, refundItem));
        when(stockCardRepositoryMock.getLastPurchase(setupProduct.getId())).thenReturn(Optional.of(setupStockCard));

        SupplyResponse updateSupply = supplyServiceMock.refundSupplyItem(supply.getId(), request);

        assertTrue(BigDecimal.valueOf(0L).compareTo(updateSupply.totalUnrefunded()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(updateSupply.totalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(500L).compareTo(updateSupply.totalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(0L).compareTo(supply.getTotalUnrefunded()) == 0);
        assertTrue(BigDecimal.valueOf(0L).compareTo(supply.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(500L).compareTo(supply.getTotalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(0L).compareTo(setupSupplier.getTotalUnpaid()) == 0);
        assertTrue(BigDecimal.valueOf(500L).compareTo(setupSupplier.getTotalPaid()) == 0);

        assertTrue(BigDecimal.valueOf(530L).compareTo(setupProduct.getBasePrice()) == 0);
        assertTrue(BigDecimal.valueOf(1L).compareTo(setupProduct.getStockQuantity()) == 0);
    }

    @Test
    @DisplayName("Update Supply: Fee increases total cost")
    public void refundSupplyItem_notFound_returnAndCalculate() {
        Supply supply = Supply.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .build();

        ItemRefundRequest request = ItemRefundRequest.builder()
                .productId(setupProduct.getId())
                .quantity(BigDecimal.valueOf(1L))
                .build();

        when(supplyItemRepositoryMock.findBySupplyIdAndProductId(supply.getId(), request.getProductId())).thenReturn(List.of());

        assertThrows(NotFoundEntityException.class, () -> supplyServiceMock.refundSupplyItem(supply.getId(), request));
    }

    @Test
    @DisplayName("Update Supply: Fee increases total cost")
    public void refundSupplyItem_refundQuantityExceedRealQuantity_returnAndCalculate() {
        Supply supply = Supply.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .build();

        setupProduct.setStockQuantity(BigDecimal.valueOf(2L));

        setupSupplyItem.setQuantity(BigDecimal.valueOf(3L));

        SupplyItem refundItem = SupplyItem.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .quantity(BigDecimal.valueOf(-1L))
                .build();

        ItemRefundRequest request = ItemRefundRequest.builder()
                .productId(setupProduct.getId())
                .quantity(BigDecimal.valueOf(3L))
                .build();

        when(supplyItemRepositoryMock.findBySupplyIdAndProductId(supply.getId(), request.getProductId())).thenReturn(List.of(setupSupplyItem, refundItem));

        assertThrows(ForbiddenRequestException.class, () -> supplyServiceMock.refundSupplyItem(supply.getId(), request));
    }

    @Test
    @DisplayName("Update Supply: Fee increases total cost")
    public void refundSupplyItem_supplyAlrCancelled_returnAndCalculate() {
        Supply supply = Supply.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .status(SupplyStatus.CANCELLED)
                .build();

        setupProduct.setStockQuantity(BigDecimal.valueOf(2L));

        setupSupplyItem.setQuantity(BigDecimal.valueOf(3L));
        setupSupplyItem.setSupply(supply);

        SupplyItem refundItem = SupplyItem.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .quantity(BigDecimal.valueOf(-1L))
                .supply(supply)
                .product(setupProduct)
                .build();

        ItemRefundRequest request = ItemRefundRequest.builder()
                .productId(setupProduct.getId())
                .quantity(BigDecimal.valueOf(1L))
                .build();

        when(supplyItemRepositoryMock.findBySupplyIdAndProductId(supply.getId(), request.getProductId())).thenReturn(List.of(setupSupplyItem, refundItem));

        assertThrows(ForbiddenRequestException.class, () -> supplyServiceMock.refundSupplyItem(supply.getId(), request));
    }

    @Test
    @DisplayName("Update Supply: Fee increases total cost")
    public void refundSupplyItem_outOfStockToRefund_returnAndCalculate() {

        Supply supply = Supply.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .subTotal(BigDecimal.valueOf(1500L))
                .grandTotal(BigDecimal.valueOf(1500L))
                .totalUnpaid(BigDecimal.valueOf(500L))
                .totalPaid(BigDecimal.valueOf(500L))
                .totalUnrefunded(BigDecimal.valueOf(0L))
                .supplier(setupSupplier)
                .build();

        setupProduct.setStockQuantity(BigDecimal.valueOf(0L));
        setupProduct.setBasePrice(BigDecimal.valueOf(515L));

        setupSupplyItem.setPrice(BigDecimal.valueOf(500L));
        setupSupplyItem.setQuantity(BigDecimal.valueOf(3L));
        setupSupplyItem.setProduct(setupProduct);
        setupSupplyItem.setSupply(supply);

        SupplyItem refundItem = SupplyItem.builder()
                .id(UuidCreator.getTimeOrderedEpochFast())
                .price(BigDecimal.valueOf(500L))
                .quantity(BigDecimal.valueOf(-1L))
                .supply(supply)
                .product(setupProduct)
                .build();

        ItemRefundRequest request = ItemRefundRequest.builder()
                .productId(setupProduct.getId())
                .quantity(BigDecimal.valueOf(1L))
                .build();

        when(supplyItemRepositoryMock.findBySupplyIdAndProductId(supply.getId(), request.getProductId())).thenReturn(List.of(setupSupplyItem, refundItem));

        assertThrows(TransactionValidationException.class, () -> supplyServiceMock.refundSupplyItem(supply.getId(), request));

    }

}