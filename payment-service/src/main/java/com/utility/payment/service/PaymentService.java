package com.utility.payment.service;

import com.utility.payment.entity.Payment;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentService {
	Mono<Payment> processPayment(Payment payment);
	Flux<Payment> getSuccessfulPayments();
}
