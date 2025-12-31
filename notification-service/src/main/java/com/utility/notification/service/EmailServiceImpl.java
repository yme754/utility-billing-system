package com.utility.notification.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.stereotype.Service;

import com.utility.notification.dto.EmailRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableReactiveMethodSecurity
public class EmailServiceImpl implements EmailService{
	private final JavaMailSender javaMailSender;
	
	@Override
	public void sendEmail(EmailRequest request) {
        log.info("Sending email to: {}", request.getTo());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("yxsh2999@gmail.com");
            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());
            javaMailSender.send(message);
            log.info("Email sent successfully!");
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }
}
