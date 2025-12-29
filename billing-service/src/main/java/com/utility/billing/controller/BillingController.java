package com.utility.billing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utility.billing.entity.Bill;
import com.utility.billing.service.BillingService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillingController {
	private final BillingService billingService;
	
	@PostMapping("/generate")
	public Mono<ResponseEntity<Bill>> generateBill(@RequestParam String connectionId,
			@RequestParam String meterId, @RequestParam String utiltyName) {
		return billingService.generateBill(connectionId, meterId, utiltyName).map(ResponseEntity::ok);
	}
}
