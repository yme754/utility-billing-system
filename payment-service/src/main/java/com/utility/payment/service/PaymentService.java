package com.utility.payment.service;

import com.utility.payment.dto.PaymentRequest;
import com.utility.payment.entity.Payment;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentService {
	Mono<Payment> processPayment(PaymentRequest request, String token);
	Flux<Payment> getSuccessfulPayments();
}
