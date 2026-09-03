package lumi.insert.app.utils.generator;

import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.net.URL;

/**
 * Utilities of Pdf's related.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
public class PdfPageTemplate extends PdfPageEventHelper{
    
    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        try {
            document.setPageSize(PageSize.A4);
            document.setMargins(72f, 72f, 0f, 30f);

            document.addAuthor("LUMI Insert"); 

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1.5f, 4.0f}); 
            headerTable.setSpacingAfter(5f);

            URL resource = getClass().getResource("/media/logo.png");

            PdfPCell logoCell;
            if(resource != null){
                Image logo = Image.getInstance(resource);
                logo.scaleToFit(50, 50);
                logo.setBackgroundColor(ExtendedColor.LIGHT_GRAY);
                logoCell = new PdfPCell(logo);
            } else {
                logoCell = new PdfPCell(new Phrase("LOGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            }
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(logoCell);
    
            PdfPCell detailsCell = new PdfPCell();
            detailsCell.setBorder(Rectangle.NO_BORDER);
            detailsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph companyName = new Paragraph("Lumi Insert",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            companyName.setAlignment(Element.ALIGN_RIGHT);
            
            Paragraph address = new Paragraph("Jl. Batam Center No. 123, Kota Batam\n" +
                    "Email: support@lumi-insert.my.id | Telp: (0778) 123456\n" +
                    "Website: www.lumi-insert.my.id",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, ExtendedColor.LIGHT_GRAY));
            address.setAlignment(Element.ALIGN_RIGHT);

            detailsCell.addElement(companyName);
            detailsCell.addElement(address);
            headerTable.addCell(detailsCell);
    
            document.add(headerTable);
    
            PdfContentByte cb = writer.getDirectContent();
            cb.setLineWidth(1.0f);
            cb.moveTo(document.leftMargin(), writer.getVerticalPosition(false) - 5);
            cb.lineTo(document.getPageSize().getWidth() - document.rightMargin(), writer.getVerticalPosition(false) - 5);
            cb.stroke();
        } catch (DocumentException | IOException de) {
            throw new ExceptionConverter(de);
        }
        
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfPTable footer = new PdfPTable(2); 
        try {
            footer.setWidths(new int[]{3, 1});
            footer.setTotalWidth(527); 
            footer.setLockedWidth(true);
            footer.getDefaultCell().setFixedHeight(20);
            footer.getDefaultCell().setBorder(Rectangle.TOP);  
 
            footer.addCell(new Phrase("Generated Document by LUMI Insert, contact internal for further question.", 
                           FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC)));
 
            PdfPCell pageNum = new PdfPCell(new Phrase(String.format("Page: %d", writer.getPageNumber()), FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD)));
            pageNum.setBorder(Rectangle.TOP);
            pageNum.setHorizontalAlignment(Element.ALIGN_RIGHT);
            footer.addCell(pageNum);
 
            footer.writeSelectedRows(0, -1, 36, 30, writer.getDirectContent());
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        }
    }
}
