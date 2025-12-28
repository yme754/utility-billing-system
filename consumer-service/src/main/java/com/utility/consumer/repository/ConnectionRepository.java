package com.utility.consumer.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.utility.consumer.entity.Connection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConnectionRepository extends ReactiveMongoRepository<Connection, String>{
	Flux<Connection> findAllByConsumerId(String consumerId);
	Mono<Boolean> existsByMeterNumber(String meterNumber);
}
