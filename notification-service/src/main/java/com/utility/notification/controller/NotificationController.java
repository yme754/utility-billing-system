package com.utility.notification.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utility.notification.dto.EmailRequest;
import com.utility.notification.service.EmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
	private final EmailService emailService;
	
	@PostMapping("/send")
	public String sendTestEmail(@RequestBody EmailRequest request) {
		emailService.sendEmail(request);
		return "Email request processed";
	}
}
