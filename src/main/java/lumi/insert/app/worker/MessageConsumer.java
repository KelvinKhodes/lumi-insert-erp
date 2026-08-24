package lumi.insert.app.worker; 

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lumi.insert.app.activitycore.entity.ActivityLog;
import lumi.insert.app.activitycore.entity.nondatabase.ActivityLogMessage;
import lumi.insert.app.activitycore.repository.ActivityLogRepository;
import lumi.insert.app.core.entity.SupplyPayment;
import lumi.insert.app.core.entity.SupplyPaymentPicture;
import lumi.insert.app.core.entity.TransactionPayment;
import lumi.insert.app.core.entity.TransactionPaymentPicture;
import lumi.insert.app.core.entity.nondatabase.CloudinaryResponse;
import lumi.insert.app.core.entity.nondatabase.EntityList;
import lumi.insert.app.core.entity.nondatabase.TransactionInvoiceMail;
import lumi.insert.app.core.entity.nondatabase.UploadStorageMessage;
import lumi.insert.app.core.repository.SupplyPaymentPictureRepository;
import lumi.insert.app.core.repository.SupplyPaymentRepository;
import lumi.insert.app.core.repository.TransactionPaymentPictureRepository;
import lumi.insert.app.core.repository.TransactionPaymentRepository;
import lumi.insert.app.exception.DatabaseInternalException;
import lumi.insert.app.exception.NotFoundEntityException;
import lumi.insert.app.exception.StorageActionException;
import lumi.insert.app.service.MailSenderService;
import lumi.insert.app.service.StorageService;

/**
 * Message Broker Consumer.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Component
@Slf4j
public class MessageConsumer {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private MailSenderService mailSenderService; 

    @Autowired
    private StorageService storageService;

    @Autowired
    private TransactionPaymentRepository trxPaymentRepository;

    @Autowired
    private TransactionPaymentPictureRepository trxPaymentPicRepository;

    @Autowired
    private SupplyPaymentRepository supplyPaymentRepository;

    @Autowired
    private SupplyPaymentPictureRepository supplyPaymentPictureRepository;

    /**
     * Listener to queues = "activity-logs".
     * <p>Save Activity Log to database</p>
     * @param activityLog
     */
    @RabbitListener(queues = "activity-logs")
    void activityLogsHandler(ActivityLogMessage activityLog){ 
        try {
            if (activityLog.getRequestId() != null) MDC.put("requestId", activityLog.getRequestId());
            log.info("Processing activity log: {}", activityLog.getId());
            ActivityLog result = activityLogRepository.save(activityLog.getActivityLog());
            log.debug("Activity log saved: {}", result);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Listener to queues = "transaction-invoice-mail".
     * <p>Send transaction invoice to mail via Service</p>
     * @param TransactionInvoiceMail
     */
    @RabbitListener(queues = "transaction-invoice-mail")
    void transactionInvoiceMailHandler(TransactionInvoiceMail request) throws MessagingException{ 
        try {
            if (request.requestId() != null) MDC.put("requestId", request.requestId());
            log.info("Processing transaction invoice mail request for transaction: {}", request.transactionId());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(request.auth(), null, null));
            mailSenderService.sendTransactionInvoice(request);
            log.debug("Transaction invoice mail sent successfully for transaction: {}", request.transactionId());
        } catch (Exception e) {
            log.error("Failed to send transaction invoice mail, with message: " + e.getMessage());
            throw e;
        } finally{
            MDC.clear();
        }
    }

    /**
     * Listener to queues = "upload-storage".
     * <p>Handle upload to 3rd storage and update related entity</p>
     * @param UploadStorageMessage
     */
    @RabbitListener(queues = "upload-storage")
    @Transactional
    void uploadStorageHandler(UploadStorageMessage request){  
        EntityList entity = request.entity();
        File file = new File(request.path());
        String publicId = null;
        
        try {
            if (request.requestId() != null) MDC.put("requestId", request.requestId());

            log.debug("Received message, trying to upload file: {}", request.path());
            log.info("Processing upload storage request for entity: {}, id: {}", entity, request.id());
            
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(request.auth(), null, null));
            if(entity == EntityList.TRANSACTION_PAYMENT){

                TransactionPayment transactionPayment = trxPaymentRepository.findById(request.id())
                    .orElseThrow(() -> new NotFoundEntityException("Transaction payment not found"));
                log.debug("Found transaction payment to update: {}", transactionPayment.getId());

                CloudinaryResponse uploadImage = storageService.uploadImage(Path.of(request.path()), "transactionPayment"); 
                publicId = uploadImage.getPublicId();
                log.info("Image uploaded successfully for transaction payment: {}", uploadImage.getSecureUrl());

                TransactionPaymentPicture transactionPaymentPicture = TransactionPaymentPicture.builder()
                    .id(UuidCreator.getTimeOrderedEpochFast())
                    .pictureUrl(uploadImage.getSecureUrl())
                    .publicId(uploadImage.getPublicId())
                    .transactionPayment(transactionPayment)
                    .build();

                TransactionPaymentPicture savedPicture = trxPaymentPicRepository.save(transactionPaymentPicture);
                log.debug("Transaction payment picture saved: {}", savedPicture);

                List<String> pictureUrl = transactionPayment.getPictureUrl();
                if(pictureUrl == null) pictureUrl = new ArrayList<>();

                pictureUrl.add(uploadImage.getSecureUrl());
                transactionPayment.setPictureUrl(pictureUrl);
                log.debug("Transaction payment updated with new picture URL");
            } else if (entity == EntityList.SUPPLY_PAYMENT){

                SupplyPayment supplyPayment = supplyPaymentRepository.findById(request.id())
                    .orElseThrow(() -> new NotFoundEntityException("Supply payment not found"));
                log.debug("Found supply payment to update: {}", supplyPayment.getId());

                CloudinaryResponse uploadImage = storageService.uploadImage(Path.of(request.path()), "supplyPayment"); 
                publicId = uploadImage.getPublicId();
                log.info("Image uploaded successfully for supply payment: {}", uploadImage.getSecureUrl());

                SupplyPaymentPicture supplyPaymentPicture = SupplyPaymentPicture.builder()
                    .id(UuidCreator.getTimeOrderedEpochFast())
                    .pictureUrl(uploadImage.getSecureUrl())
                    .publicId(uploadImage.getPublicId())
                    .supplyPayment(supplyPayment)
                    .build();
                
                SupplyPaymentPicture savedPicture = supplyPaymentPictureRepository.save(supplyPaymentPicture);
                log.debug("Supply payment picture saved: {}", savedPicture);

                List<String> pictureUrl = supplyPayment.getPictureUrl();
                if(pictureUrl == null) pictureUrl = new ArrayList<>();
                
                pictureUrl.add(uploadImage.getSecureUrl());
                supplyPayment.setPictureUrl(pictureUrl);
                log.debug("Supply payment updated with new picture URL");
            }

        } catch (IOException e) {
            log.error("Upload failed for entity {} with id {}, messages: {}", entity, request.id(), e.getMessage());
            throw new StorageActionException("Server couldn't complete the request due to internal problem, try again or contact developer");
        } catch (Exception e) { 
            log.error("Save to database failed for entity {} with id {}, attempting to delete image at storage. Messages: {}", entity, request.id(), e.getMessage()); 

            if(publicId != null) {
                if(!(storageService.deleteImage(publicId))) log.error("Failed to delete image with publicId: {}", publicId);
            }

            throw new DatabaseInternalException("Server couldn't complete the request due to internal problem, try again or contact developer");
        } finally{
            file.deleteOnExit();
            log.debug("Upload storage handler completed for entity: {}, id: {}", entity, request.id());
            MDC.clear();
        }
            
    } 

}
