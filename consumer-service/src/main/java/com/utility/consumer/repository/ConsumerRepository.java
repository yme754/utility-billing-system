package com.utility.consumer.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.utility.consumer.entity.Consumer;

import reactor.core.publisher.Mono;

@Repository
public interface ConsumerRepository extends ReactiveMongoRepository<Consumer, String>{
	Mono<Consumer> findByUserId(String userId);
	Mono<Consumer> findByEmail(String email);
}
