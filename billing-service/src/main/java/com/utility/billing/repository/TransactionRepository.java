package com.utility.billing.repository;

import com.utility.billing.entity.Transaction;

import reactor.core.publisher.Flux;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String>{
	Flux<Transaction> findByBillId(String billId);
}
