package com.utility.notification.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utility.notification.dto.EmailRequest;
import com.utility.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
	private final EmailService emailService;
	
	@PostMapping("/send")
	@PreAuthorize("hasRole('ADMIN')")
	public Mono<String> sendTestEmail(@RequestBody EmailRequest request) {
        emailService.sendEmail(request);
        return Mono.just("Email request processed");
    }
}
