package lumi.insert.app.service.implement;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.core.io.ByteArrayResource; 
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException; 
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.core.entity.Transaction;
import lumi.insert.app.core.entity.nondatabase.TransactionInvoiceMail;
import lumi.insert.app.core.repository.TransactionRepository;
import lumi.insert.app.core.repository.projection.ProductOutOfStock;
import lumi.insert.app.dto.response.TransactionDetailResponse;
import lumi.insert.app.dto.response.TransactionItemStatisticResponse;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.mapper.AllTransactionMapper;
import lumi.insert.app.service.MailSenderService;
import lumi.insert.app.service.PdfService;
import lumi.insert.app.service.ProductService;
import lumi.insert.app.service.TransactionItemService;

/**
 * Implementation of {@link MailSenderService} for handling automated email communications.
 * <p>
 * This service orchestrates the generation of document-based emails, support attachments.
 * It supports HTML-formatted templates and multi-part messaging for binary attachments.
 * </p>
 *
 * @author KelvinKhodes
 * @since 1.0.0
 */
@Service
@Slf4j
public class MailSenderServiceImpl implements MailSenderService {

    @Autowired
    JavaMailSender sender;

    @Autowired
    PdfService pdfService;

    @Autowired
    ProductService productService;

    @Autowired
    TransactionItemService transactionItemService;

    @Autowired
    TransactionRepository transactionRepository;
 
    @Autowired
    AllTransactionMapper allTransactionMapper;

    private String template = 
            "<div style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: auto; background: white; padding: 40px; text-align: center; border-radius: 8px;'>" +
                    "<img src='https://github.githubassets.com/assets/pull-shark-default-498c279a747d.png' style='width: 150px; margin-bottom: 20px;' />" +
                    "<h2 style='color: #333;'>Thanks for your transaction!</h2>" +
                    "<p style='color: #666; line-height: 1.6;'>We'd like to share your recent transaction's invoice, for further information, you can find us on: </p>" +
                    "<br>" +
                    "<a href='https://support.lumi-insert.my.id' style='background-color: #24e5d0; color: #003333; padding: 15px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Launch a capsule</a>" +
                    "<hr style='border: 0; border-top: 1px solid #eee; margin: 40px 0;'>" + 
                    "<p style='text-align: left;'>LUMI Insert Inc.</p>" +
                "</div>" +
     "</div>";

     /**
     * Sends a transaction invoice to a customer via email.
     * <p>
     * This method retrieves full transaction details, converts them into a PDF stream, 
     * and dispatches an HTML email with the PDF attached.
     * </p>
     *
     * @param request the mail request containing the recipient's email and transaction ID.
     * @throws MessagingException      if the email construction or SMTP delivery fails.
     * @throws NotFoundEntityException if the transaction ID does not exist in the database.
     */
    @Override
    public void sendTransactionInvoice(TransactionInvoiceMail request) throws MessagingException { 
        log.info("Preparing transaction invoice email to={} transactionId={}", request.email(), request.transactionId());
        Transaction data = transactionRepository.findByIdDetail(request.transactionId())
            .orElseThrow(() -> new NotFoundEntityException(""));
        
        TransactionDetailResponse dataDetail = allTransactionMapper.createTransactionDetailResponseDto(data);
        ByteArrayInputStream pdfByte = pdfService.exportTransactionWithItems(dataDetail);

        MimeMessage mimeMessage = sender.createMimeMessage(); 
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true); 
        helper.setTo(request.email());
        helper.setFrom("noreply@lumi-insert.my.id");
        helper.setSubject("Transaction Invoice - " + dataDetail.invoiceId());
        helper.setText(template, true); 
        helper.addAttachment(dataDetail.customerName() + "-" + dataDetail.invoiceId() + ".pdf", new ByteArrayResource(pdfByte.readAllBytes()));
         
        sender.send(mimeMessage);
        log.info("Transaction invoice email sent to={} transactionId={}", request.email(), request.transactionId());
    }

    /**
     * Generates and sends a performance report of products to the system owner.
     * <p>
     * The report includes transaction statistics and a list of out-of-stock items 
     * for a specified period, bundled as a PDF attachment.
     * </p>
     *
     * @param startDate the beginning of the reporting period.
     * @param endDate   the end of the reporting period.
     * @throws MessagingException if the email delivery fails.
     */
    @Override
    public void sendProductsStatistic(LocalDateTime startDate, LocalDateTime endDate) throws MessagingException { 
        log.info("Preparing products statistic email for period {} to {}", startDate, endDate);
        TransactionItemStatisticResponse transactionItemStats = transactionItemService.getTransactionItemStats(startDate, endDate);

        List<ProductOutOfStock> outOfStockProducts = productService.getOutOfStockProducts();

        ByteArrayInputStream pdfByte = pdfService.exportProductsStatistic(transactionItemStats, outOfStockProducts,startDate, endDate);

        MimeMessage mimeMessage = sender.createMimeMessage(); 
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true); 
        helper.setTo("kelvinkho050@gmail.com");
        helper.setFrom("noreply@lumi-insert.my.id");
        helper.setSubject("Products statistics - Daily");
        helper.setText(template, true); 
        helper.addAttachment( "Products statistics" + startDate + "-" + endDate + ".pdf", new ByteArrayResource(pdfByte.readAllBytes()));
         
        sender.send(mimeMessage);
        log.info("Products statistics email sent for period {} to {}", startDate, endDate);
    }
    
}
