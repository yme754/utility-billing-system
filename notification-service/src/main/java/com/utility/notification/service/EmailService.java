package com.utility.notification.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.utility.notification.dto.EmailRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
	private final JavaMailSender javaMailSender;
	public void sendEmail(EmailRequest emailRequest) {
		try {
			log.info("Sending email to: {}", emailRequest.getTo());
			MimeMessage message = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setTo(emailRequest.getTo());
			helper.setSubject(emailRequest.getSubject());
			helper.setText(emailRequest.getBody(), true);
			javaMailSender.send(message);
			log.info("Email sent successfully!");
		} catch(MessagingException e) {
			log.error("Failed to send email", e);
		}
	}
}
