package lumi.insert.app.service.implement;
  
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle; 
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.aspect.annotation.ActivityLogger;
import lumi.insert.app.core.entity.nondatabase.ActivityAction; 
import lumi.insert.app.dto.response.SupplyResponse;
import lumi.insert.app.dto.response.TransactionResponse;
import lumi.insert.app.service.XlsxService;

/**
 * Implementation of {@link XlsxService}.
 * <p>
 * This service handles the making of templated pdf document from input data.
 * </p>
 * * <h3>Key Responsibilities:</h3>
 * <ul>
 * <li><b>Generating XLSX :</b> Converting a given data source into bytes.</li>    
 * </ul>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Slf4j
@Service
public class XlsxServiceImpl implements XlsxService{

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);

    /**
     * Convert list of {@link TransactionResponse} into templated XLSX and pass it to given {@link OutputStream}.
     * @param datas a source of data from list {@link TransactionResponse}.
     * @param outputStream stream used to put the created bytes(xlsx).
     */ 
    @Override
    @ActivityLogger(
        entityName = "transactions",
        action = ActivityAction.EXPORT_DATA,
        actionMessage = "Transaction history exported as XLSX"
    )
    public void exportTransactions(List<TransactionResponse> datas, OutputStream outputStream) {
        log.info("Exporting {} transactions to XLSX", datas.size());
        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) { 
            SXSSFSheet sheet = workbook.createSheet("Transactions");
            SXSSFRow titleRow = sheet.createRow(0);
            SXSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Transaction List: ");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 14)); 

            short px12 = 12;
            Font font = workbook.createFont();
            font.setBold(true);

            font.setFontHeightInPoints(px12);

            CellStyle defaultCenter = workbook.createCellStyle();
            defaultCenter.setAlignment(HorizontalAlignment.CENTER); 
            defaultCenter.setVerticalAlignment(VerticalAlignment.CENTER); 
            defaultCenter.setFillBackgroundColor(HSSFColor.HSSFColorPredefined.LIGHT_BLUE.getIndex());
            defaultCenter.setFont(font);
            titleCell.setCellStyle(defaultCenter);

            SXSSFRow headerRow = sheet.createRow(1);
            headerRow.createCell(0).setCellValue("No");
            headerRow.createCell(1).setCellValue("Invoice");
            headerRow.createCell(2).setCellValue("Customer");
            headerRow.createCell(3).setCellValue("Total Item");
            headerRow.createCell(4).setCellValue("Fee");
            headerRow.createCell(5).setCellValue("Discount");
            headerRow.createCell(6).setCellValue("Sub Total");
            headerRow.createCell(7).setCellValue("Grand Total");
            headerRow.createCell(8).setCellValue("Unpaid");
            headerRow.createCell(9).setCellValue("Paid");
            headerRow.createCell(10).setCellValue("Unrefund");
            headerRow.createCell(11).setCellValue("Refund");
            headerRow.createCell(12).setCellValue("Status");
            headerRow.createCell(13).setCellValue("Transaction Date");
            headerRow.createCell(14).setCellValue("Last Update"); 
            
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("[$Rp-421]#,##0.00;[RED]-[$Rp-421]#,##0.00"));    

            for (int x = 1; x < 15; x++) {
                if(x > 11){
                    sheet.setColumnWidth(x,  24 * 256);
                } else {
                    sheet.setColumnWidth(x,  20 * 256); 
                    if(x != 3) sheet.setDefaultColumnStyle(x, currencyStyle);
                }
            }
            headerRow.setRowStyle(defaultCenter);

            sheet.setColumnWidth(1,  24 * 256);
            sheet.setColumnWidth(2,  28 * 256);
 
            int i = 2;
            for (TransactionResponse data : datas) {
                SXSSFRow row = sheet.createRow(i);  

                row.createCell(0).setCellValue(i-1);
                row.createCell(1).setCellValue(data.invoiceId());
                row.createCell(2).setCellValue(data.customerName());
                row.createCell(3).setCellValue(data.totalItems());
                row.createCell(4).setCellValue(data.totalFee().longValue());
                row.createCell(5).setCellValue(data.totalDiscount().longValue());
                row.createCell(6).setCellValue(data.subTotal().longValue());
                row.createCell(7).setCellValue(data.grandTotal().longValue());
                row.createCell(8).setCellValue(data.totalUnpaid().longValue());
                row.createCell(9).setCellValue(data.totalPaid().longValue());
                row.createCell(10).setCellValue(data.totalUnrefunded().longValue());
                row.createCell(11).setCellValue(data.totalRefunded().longValue());
                row.createCell(12).setCellValue(String.valueOf(data.status()));
                row.createCell(13).setCellValue(data.createdAt().format(dateTimeFormatter));
                row.createCell(14).setCellValue(data.updatedAt().format(dateTimeFormatter)); 

                i++;
            } 

            SXSSFRow totalRow = sheet.createRow(i); 
            totalRow.createCell(2).setCellValue("SUM: "); 
            totalRow.createCell(3).setCellFormulaImpl("=SUM(D3:D" + i); 
            totalRow.createCell(4).setCellFormulaImpl("=SUM(E3:E" + i); 
            totalRow.createCell(5).setCellFormulaImpl("=SUM(F3:F" + i); 
            totalRow.createCell(6).setCellFormulaImpl("=SUM(G3:G" + i); 
            totalRow.createCell(7).setCellFormulaImpl("=SUM(H3:H" + i); 
            totalRow.createCell(8).setCellFormulaImpl("=SUM(I3:I" + i); 
            totalRow.createCell(9).setCellFormulaImpl("=SUM(J3:J" + i); 
            totalRow.createCell(10).setCellFormulaImpl("=SUM(K3:K" + i); 
            totalRow.createCell(11).setCellFormulaImpl("=SUM(L3:L" + i); 

            sheet.createFreezePane(0, 2);
            workbook.write(outputStream);
        } catch (Exception e) {
           log.warn("Exception triggered during exporting stream to client, with message: {}", e.getLocalizedMessage());
        }
    }

    /**
     * Convert list of {@link SupplyResponse} into templated XLSX and pass it to given {@link OutputStream}.
     * @param datas a source of data from list {@link SupplyResponse}.
     * @param outputStream stream used to put the created bytes(xlsx).
     */ 
    @Override
    @ActivityLogger(
        entityName = "supplies",
        action = ActivityAction.EXPORT_DATA,
        actionMessage = "Supply history exported as XLSX"
    )
    public void exportSupplies(List<SupplyResponse> datas, OutputStream outputStream) {
        log.info("Exporting {} supplies to XLSX", datas.size());
         try (SXSSFWorkbook workbook = new SXSSFWorkbook()) { 
            SXSSFSheet sheet = workbook.createSheet("Supplies");
            SXSSFRow titleRow = sheet.createRow(0);
            SXSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Supply List: ");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 14)); 
            
            short px12 = 12;
            Font font = workbook.createFont();
            font.setBold(true);
            
            font.setFontHeightInPoints(px12);

            CellStyle defaultCenter = workbook.createCellStyle();
            defaultCenter.setAlignment(HorizontalAlignment.CENTER); 
            defaultCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            defaultCenter.setFillBackgroundColor(HSSFColor.HSSFColorPredefined.GOLD.getIndex());
            defaultCenter.setFont(font); 
            titleCell.setCellStyle(defaultCenter);

            SXSSFRow headerRow = sheet.createRow(1);
            headerRow.createCell(0).setCellValue("No");
            headerRow.createCell(1).setCellValue("Invoice");
            headerRow.createCell(2).setCellValue("Supplier");
            headerRow.createCell(3).setCellValue("Total Item");
            headerRow.createCell(4).setCellValue("Fee");
            headerRow.createCell(5).setCellValue("Discount");
            headerRow.createCell(6).setCellValue("Sub Total");
            headerRow.createCell(7).setCellValue("Grand Total");
            headerRow.createCell(8).setCellValue("Unpaid");
            headerRow.createCell(9).setCellValue("Paid");
            headerRow.createCell(10).setCellValue("Unrefund");
            headerRow.createCell(11).setCellValue("Refund");
            headerRow.createCell(12).setCellValue("Status");
            headerRow.createCell(13).setCellValue("Transaction Date"); 

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("[$Rp-421]#,##0.00;[RED]-[$Rp-421]#,##0.00"));   
            for (int x = 1; x < 15; x++) {
                if(x > 11){
                    sheet.setColumnWidth(x,  24 * 256);
                } else {
                    sheet.setColumnWidth(x,  20 * 256); 
                    sheet.setDefaultColumnStyle(x, currencyStyle); 
                }
            }

            headerRow.setRowStyle(defaultCenter);

            int i = 2;
            for (SupplyResponse data : datas) {
                SXSSFRow row = sheet.createRow(i);  

                row.createCell(0).setCellValue(i-1);
                row.createCell(1).setCellValue(data.invoiceId());
                row.createCell(2).setCellValue(data.supplierName());
                row.createCell(3).setCellValue(data.totalItems());
                row.createCell(4).setCellValue(data.totalFee().longValue());
                row.createCell(5).setCellValue(data.totalDiscount().longValue());
                row.createCell(6).setCellValue(data.subTotal().longValue());
                row.createCell(7).setCellValue(data.grandTotal().longValue());
                row.createCell(8).setCellValue(data.totalUnpaid().longValue());
                row.createCell(9).setCellValue(data.totalPaid().longValue());
                row.createCell(10).setCellValue(data.totalUnrefunded().longValue());
                row.createCell(11).setCellValue(data.totalRefunded().longValue());
                row.createCell(12).setCellValue(String.valueOf(data.status()));
                row.createCell(13).setCellValue(data.createdAt().format(dateTimeFormatter)); 

                i++;
            }

            SXSSFRow totalRow = sheet.createRow(i); 
            totalRow.createCell(2).setCellValue("SUM: "); 
            totalRow.createCell(3).setCellFormulaImpl("=SUM(D3:D" + i); 
            totalRow.createCell(4).setCellFormulaImpl("=SUM(E3:E" + i); 
            totalRow.createCell(5).setCellFormulaImpl("=SUM(F3:F" + i); 
            totalRow.createCell(6).setCellFormulaImpl("=SUM(G3:G" + i); 
            totalRow.createCell(7).setCellFormulaImpl("=SUM(H3:H" + i); 
            totalRow.createCell(8).setCellFormulaImpl("=SUM(I3:I" + i); 
            totalRow.createCell(9).setCellFormulaImpl("=SUM(J3:J" + i); 
            totalRow.createCell(10).setCellFormulaImpl("=SUM(K3:K" + i); 
            totalRow.createCell(11).setCellFormulaImpl("=SUM(L3:L" + i); 

            
            sheet.createFreezePane(0, 2);
            workbook.write(outputStream);
        } catch (Exception e) {
            log.warn("Exception triggered during exporting stream to client, with message: {}", e.getLocalizedMessage());
        }
    }
    
}
