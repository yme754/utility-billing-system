package com.utility.notification.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.utility.notification.dto.EmailRequest;
import com.utility.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {
	private final EmailService emailService;
	
	@KafkaListener(topics= "notification-topic", groupId = "notification-group")
	public void handleNotification(EmailRequest emailRequest) {
		try {
	        log.info("Received Kafka Message for: {}", emailRequest.getTo());
	        emailService.sendEmail(emailRequest);
	    } catch (Exception e) {
	        log.error("Error processing Kafka message: {}", e.getMessage());
	    }
	}
}