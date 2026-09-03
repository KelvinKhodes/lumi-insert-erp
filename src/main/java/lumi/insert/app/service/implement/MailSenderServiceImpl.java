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

    private final String transactionTemplate =
        "<div style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; padding: 40px 20px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; padding: 40px; border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);'>" +

            "<!-- Brand Header -->" +
            "<table width='100%' border='0' cellspacing='0' cellpadding='0' style='margin-bottom: 24px;'>" +
            "<tr>" +
            "<td align='left' valign='middle'>" +
            "<img src='https://github.githubassets.com/assets/pull-shark-default-498c279a747d.png' alt='Logo' style='width: 42px; height: 42px; display: block;' />" +
            "</td>" +
            "<td align='right' valign='middle'>" +
            "<span style='font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: #059669; background-color: #ecfdf5; padding: 6px 12px; border-radius: 20px;'>Payment Confirmed</span>" +
            "</td>" +
            "</tr>" +
            "</table>" +

            "<!-- Title & Context -->" +
            "<h2 style='color: #0f172a; font-size: 22px; font-weight: 700; margin: 0 0 8px 0; letter-spacing: -0.02em;'>Thank you for your purchase!</h2>" +

            "<p style='color: #334155; font-size: 15px; line-height: 1.6; margin: 0 0 16px 0;'>Hello,</p>" +
            "<p style='color: #334155; font-size: 15px; line-height: 1.6; margin: 0 0 28px 0;'>We have successfully processed your transaction. Your official invoice has been generated and is attached to this email as a PDF document for your records.</p>" +

            "<!-- Call to Action -->" +
            "<div style='text-align: center; margin-bottom: 36px;'>" +
            "<a href='https://support.lumi-insert.my.id' style='background-color: #0f172a; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600; display: inline-block; letter-spacing: 0.01em;'>Visit Support Center &rarr;</a>" +
            "</div>" +

            "<hr style='border: 0; border-top: 1px solid #f1f5f9; margin: 0 0 24px 0;' />" +

            "<!-- Footer -->" +
            "<div style='text-align: center; font-size: 12px; color: #94a3b8; line-height: 1.5;'>" +
            "<p style='margin: 0 0 4px 0; font-weight: 500;'>LUMI Insert Inc. &bull; Official Billing Notification</p>" +
            "<p style='margin: 0;'>If you have any questions regarding this charge, please contact our support team.</p>" +
            "</div>" +

            "</div>" +
            "</div>";

    private final String reportTemplate =
        "<div style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; padding: 40px 20px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; padding: 40px; border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);'>" +

            "<!-- Brand Header -->" +
            "<table width='100%' border='0' cellspacing='0' cellpadding='0' style='margin-bottom: 24px;'>" +
            "<tr>" +
            "<td align='left' valign='middle'>" +
            "<img src='https://github.githubassets.com/assets/pull-shark-default-498c279a747d.png' alt='Logo' style='width: 42px; height: 42px; display: block;' />" +
            "</td>" +
            "<td align='right' valign='middle'>" +
            "<span style='font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; background-color: #f1f5f9; padding: 6px 12px; border-radius: 20px;'>Executive Report</span>" +
            "</td>" +
            "</tr>" +
            "</table>" +

            "<!-- Title & Context -->" +
            "<h2 style='color: #0f172a; font-size: 22px; font-weight: 700; margin: 0 0 8px 0; letter-spacing: -0.02em;'>Performance Report Ready</h2>" +

            "<p style='color: #334155; font-size: 15px; line-height: 1.6; margin: 0 0 16px 0;'>Hello Owner,</p>" +
            "<p style='color: #334155; font-size: 15px; line-height: 1.6; margin: 0 0 28px 0;'>Your periodic performance statistics report has been generated. Please find the detailed PDF document attached to this email for your review.</p>" +

            "<!-- Call to Action -->" +
            "<div style='text-align: center; margin-bottom: 36px;'>" +
            "<a href='https://lumi-insert.my.id' style='background-color: #0f172a; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600; display: inline-block; letter-spacing: 0.01em;'>Open Interactive Dashboard &rarr;</a>" +
            "</div>" +

            "<hr style='border: 0; border-top: 1px solid #f1f5f9; margin: 0 0 24px 0;' />" +

            "<!-- Footer -->" +
            "<div style='text-align: center; font-size: 12px; color: #94a3b8; line-height: 1.5;'>" +
            "<p style='margin: 0 0 4px 0; font-weight: 500;'>LUMI Insert Inc. &bull; Automated System Intelligence</p>" +
            "<p style='margin: 0;'>You are receiving this automated email because you are registered as an account administrator.</p>" +
            "</div>" +

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
        helper.setText(transactionTemplate, true);
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
        helper.setText(reportTemplate, true);
        helper.addAttachment( "Products statistics" + startDate + "-" + endDate + ".pdf", new ByteArrayResource(pdfByte.readAllBytes()));
         
        sender.send(mimeMessage);
        log.info("Products statistics email sent for period {} to {}", startDate, endDate);
    }
    
}
