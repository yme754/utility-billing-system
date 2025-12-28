package com.utility.utility.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.utility.utility.entity.Utility;

import reactor.core.publisher.Mono;

public interface UtilityRepository extends ReactiveMongoRepository<Utility, String>{
	Mono<Utility> findByName(String name);
}
