package lumi.insert.app.service.pdf;
 
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lumi.insert.app.utils.generator.NumberFormatter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.f4b6a3.uuid.UuidCreator;

import lumi.insert.app.core.entity.nondatabase.SupplyStatus;
import lumi.insert.app.core.entity.nondatabase.TransactionStatus;
import lumi.insert.app.core.repository.projection.ProductOutOfStock;
import lumi.insert.app.core.repository.projection.ProductRefund;
import lumi.insert.app.core.repository.projection.ProductSale;
import lumi.insert.app.dto.response.ProductName; 
import lumi.insert.app.dto.response.SupplyDetailResponse;
import lumi.insert.app.dto.response.SupplyItemResponse;
import lumi.insert.app.dto.response.TransactionDetailResponse;
import lumi.insert.app.dto.response.TransactionItemResponse;
import lumi.insert.app.dto.response.TransactionItemStatisticResponse;
import lumi.insert.app.service.implement.PdfServiceImpl;

@ExtendWith(MockitoExtension.class)
public class PdfServiceTest {

    @InjectMocks
    private PdfServiceImpl pdfService;

    @Test
    void testExportSupplyWithItems_ShouldReturnValidPdfContent() throws IOException {

        ProductName productName = ProductName.builder()
        .id(1L)
        .name("Shoes")
        .build();

        SupplyItemResponse item1 = new SupplyItemResponse(
            UuidCreator.getTimeOrderedEpochFast(), 
            productName, 
            BigDecimal.valueOf(10L), 
            BigDecimal.valueOf(10L),
             null
        );
        
        SupplyItemResponse item2 = new SupplyItemResponse(
            UuidCreator.getTimeOrderedEpochFast(), 
            productName, 
            BigDecimal.valueOf(10L), 
            BigDecimal.valueOf(10L),
             null
        );

        SupplyDetailResponse mockData = new SupplyDetailResponse(
            UuidCreator.getTimeOrderedEpochFast(), 
            "INV-999",
            UuidCreator.getTimeOrderedEpochFast(), 
            "Acme Supply Co.", 
                List.of(item1, item2),
                2L,
                BigDecimal.valueOf(2L),    
                BigDecimal.valueOf(20L),  
                BigDecimal.valueOf(200L),  
                BigDecimal.valueOf(182L),  
                BigDecimal.valueOf(182L), 
                BigDecimal.valueOf(0L),   
                BigDecimal.valueOf(0L),
                BigDecimal.valueOf(0L),
                SupplyStatus.UNPAID,
                null,
                null,
                LocalDateTime.now()
        );

        try(MockedStatic<NumberFormatter> mockedHelper = Mockito.mockStatic(NumberFormatter.class)){
            mockedHelper.when(() -> NumberFormatter.convertToCurrency(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());

            mockedHelper.when(() -> NumberFormatter.normalizeBigDecimal(any(), anyInt())).thenAnswer(invocation -> invocation.getArgument(0));
            ByteArrayInputStream result = pdfService.exportSupplyWithItems(mockData);

            assertNotNull(result);
            byte[] pdfBytes = result.readAllBytes();
            assertTrue(pdfBytes.length > 0);

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                assertTrue(text.contains("Supply Order"));
                assertTrue(text.contains("No. Invoice: INV-999"));
                assertTrue(text.contains("Supplier: Acme Supply Co."));

                assertTrue(text.contains("Shoes"));
                assertTrue(text.contains("10"));

                assertTrue(text.contains("Subtotal"));
                assertTrue(text.contains("200"));
                assertTrue(text.contains("Grandtotal"));
                assertTrue(text.contains("182"));

                assertTrue(text.contains("Warehouse Staff"));
            }
        }
    }

    @Test
    void exportTransactionWithItems_validData_ShouldReturnValidPdfContent() throws IOException {  

        TransactionItemResponse item1 = new TransactionItemResponse(
            UuidCreator.getTimeOrderedEpochFast(), 
            null,
            1L,
            "Shoes",
            null,
            BigDecimal.valueOf(10L), 
            BigDecimal.valueOf(10L),
            null, 
            null
        );
        
        TransactionItemResponse item2 = new TransactionItemResponse(
            UuidCreator.getTimeOrderedEpochFast(), 
            null,
            2L,
            "Watch",
            null,
            BigDecimal.valueOf(100L), 
            BigDecimal.valueOf(10L),
            null, 
            null
        );

        TransactionDetailResponse mockData = new TransactionDetailResponse(
            UuidCreator.getTimeOrderedEpochFast(), 
            "INV-999",
            UuidCreator.getTimeOrderedEpochFast(), 
            "Acme Supply Co.", 
                List.of(item1, item2),
                2L,
                BigDecimal.valueOf(2L),    
                BigDecimal.valueOf(20L),  
                BigDecimal.valueOf(200L),  
                BigDecimal.valueOf(182L),  
                BigDecimal.valueOf(182L), 
                BigDecimal.valueOf(0L),   
                BigDecimal.valueOf(0L),
                BigDecimal.valueOf(0L),
                TransactionStatus.PROCESS,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        try(MockedStatic<NumberFormatter> mockedHelper = Mockito.mockStatic(NumberFormatter.class)) {
            mockedHelper.when(() -> NumberFormatter.convertToCurrency(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());

            mockedHelper.when(() -> NumberFormatter.normalizeBigDecimal(any(), anyInt())).thenAnswer(invocation -> invocation.getArgument(0));
            ByteArrayInputStream result = pdfService.exportTransactionWithItems(mockData);

            assertNotNull(result);
            byte[] pdfBytes = result.readAllBytes();
            assertTrue(pdfBytes.length > 0);

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                assertTrue(text.contains("Transaction Order"));
                assertTrue(text.contains("No. Invoice: INV-999"));
                assertTrue(text.contains("Customer: Acme Supply Co."));

                assertTrue(text.contains("Shoes"));
                assertTrue(text.contains("10"));

                assertTrue(text.contains("Watch"));

                assertTrue(text.contains("Subtotal"));
                assertTrue(text.contains("200"));
                assertTrue(text.contains("Grandtotal"));
                assertTrue(text.contains("182"));

                assertTrue(text.contains("Cashier Staff"));
            }
        }
    }

    
    @Test
    void testExportProductsStatistic_Success() throws IOException { 
        ProductSale sales = new ProductSale("Tea", BigDecimal.valueOf(100L));
        ProductRefund refunds = new ProductRefund("Shoes", BigDecimal.valueOf(5L));
        ProductOutOfStock oosProduct = new ProductOutOfStock(1L, "Mirror", BigDecimal.valueOf(2L), BigDecimal.valueOf(10L));

        TransactionItemStatisticResponse statsResponse = TransactionItemStatisticResponse.builder()
        .productRefunds(List.of(refunds))
        .productSales(List.of(sales))
        .build();

        LocalDateTime now = LocalDateTime.now();
 
        ByteArrayInputStream bais = pdfService.exportProductsStatistic(
                statsResponse, 
                List.of(oosProduct), 
                now.minusDays(7), 
                now
        );
 
        assertNotNull(bais);
        assertTrue(bais.available() > 0);
 
        try (PDDocument document = Loader.loadPDF(bais.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(document);
 
            assertTrue(pdfText.contains("Products statistic"));
            assertTrue(pdfText.contains("Shoes"));
            assertTrue(pdfText.contains("Pcs"));
            assertTrue(pdfText.contains("Tea"));
             
            assertTrue(document.getNumberOfPages() > 0);
        }
    }

    @Test
    void testExportProductsStatistic_EmptyList_ShouldStillGeneratePdf() throws IOException {
        TransactionItemStatisticResponse statsResponse = TransactionItemStatisticResponse.builder()
        .productRefunds(List.of())
        .productSales(List.of())
        .build();
        
        ByteArrayInputStream bais = pdfService.exportProductsStatistic(
                statsResponse, List.of(), LocalDateTime.now(), LocalDateTime.now()
        );

        assertNotNull(bais);
        assertTrue(bais.available() > 0);
    }
}
