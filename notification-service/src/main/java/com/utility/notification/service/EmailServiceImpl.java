package com.utility.notification.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.utility.notification.dto.EmailRequest;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService{
	private final JavaMailSender javaMailSender;
	
	@Override
	public void sendEmail(EmailRequest request) {
        log.info("Sending email to: {}", request.getTo());
        try {
        	MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("yxsh2999@gmail.com");
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            String htmlContent = String.format(
                "<div style='font-family:sans-serif; padding:20px; border:1px solid #eee; border-radius:10px;'>" +
                "<h2 style='color:#2563EB;'>Utilix Notification</h2>" +
                "<p>%s</p>" +
                "<hr style='border:none; border-top:1px solid #eee; margin:20px 0;'>" +
                "<p style='font-size:12px; color:#666;'>This is an automated message from the Utilix Billing System.</p>" +
                "</div>", 
                request.getBody().replace("\n", "<br>")
            );

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            log.info("HTML Email sent successfully!");
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }
}
