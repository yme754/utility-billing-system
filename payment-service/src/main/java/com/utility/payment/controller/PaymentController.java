package com.utility.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utility.payment.entity.Payment;
import com.utility.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
	private final PaymentService paymentService;
	
	@PostMapping("/pay")
	@PreAuthorize("hasRole('CONSUMER')")
	public Mono<ResponseEntity<Payment>> makePayment(@RequestBody Payment payment) {
		return paymentService.processPayment(payment).map(ResponseEntity::ok);
	}
	
	@GetMapping("/history")
    @PreAuthorize("hasAnyRole('ACCOUNTS_OFFICER', 'ADMIN')")
    public Mono<ResponseEntity<Flux<Payment>>> getPaymentHistory() {
        return Mono.just(ResponseEntity.ok(paymentService.getSuccessfulPayments()));
    }
}
