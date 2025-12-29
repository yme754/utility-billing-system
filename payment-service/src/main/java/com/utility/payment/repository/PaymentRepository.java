package com.utility.payment.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.utility.payment.entity.Payment;

import reactor.core.publisher.Flux;

public interface PaymentRepository extends ReactiveMongoRepository<Payment, String>{
	Flux<Payment> findByBillId(String billId);
	Flux<Payment> findByStatus(String status);
}
