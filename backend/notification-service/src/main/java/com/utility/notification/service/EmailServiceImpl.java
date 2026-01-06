package com.utility.notification.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.utility.notification.dto.EmailRequest;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService{
	private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendEmail(EmailRequest request) {
        log.info("Processing email for: {}", request.getTo());
        
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            
            if (request.isInvoice()) {
                helper.setText("<h3>Hello,</h3><p>Your monthly utility bill has been generated. Please find the attached PDF invoice.</p>", true);                
                byte[] pdfContent = generateInvoicePdf(request);
                helper.addAttachment("Invoice_" + (request.getBillId() != null ? request.getBillId() : "New") + ".pdf", new ByteArrayResource(pdfContent));
            } else {
                helper.setText(request.getBody(), false);
            }

            sendWithRetry(message, request.getTo());
        } catch (Exception e) {
            log.error("Fatal error constructing email for {}", request.getTo(), e);
        }
    }

    private void sendWithRetry(MimeMessage message, String recipient) {
        try {
            javaMailSender.send(message);
            log.info("Email sent successfully to {}", recipient);
        } catch (MailException e) {
            log.warn("Attempt 1 failed for {}. Retrying in 1 second... Error: {}", recipient, e.getMessage());
            
            try {
                Thread.sleep(1000);                 
                javaMailSender.send(message); 
                log.info("Email sent successfully on Attempt 2 to {}", recipient);
            } catch (InterruptedException ie) {
                log.error("Email retry thread was interrupted for recipient: {}", recipient, ie);
                Thread.currentThread().interrupt(); 
            } catch (Exception retryException) {
                log.error("Failed to send email after retry to: {}", recipient, retryException);
            }
        }
    }

    private byte[] generateInvoicePdf(EmailRequest request) throws DocumentException, IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();        
            
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLUE);
            document.add(new Paragraph("UTILIX", titleFont));
            document.add(new Paragraph("Invoice Date: " + LocalDate.now()));
            document.add(new Paragraph("--------------------------------------------------"));
            
            PdfPTable table = new PdfPTable(2);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            table.addCell("Bill ID");
            table.addCell(request.getBillId() != null ? request.getBillId() : "N/A");
            
            table.addCell("Details");
            table.addCell(request.getBody() != null ? request.getBody() : "Monthly Usage Charge");
            
            table.addCell("Total Amount Due");
            table.addCell("INR " + (request.getAmount() != null ? request.getAmount() : "0.00"));
            
            document.add(table);
            document.add(new Paragraph("\nPayment is due within 30 days. Please pay promptly to avoid service disconnection or fine."));
            document.add(new Paragraph("\nThank you for choosing Utilix!"));
            
            document.close();
            return baos.toByteArray();
        }
    }
}