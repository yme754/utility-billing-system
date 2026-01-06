package com.utility.utility.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.utility.utility.entity.Tariff;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TariffRepository extends ReactiveMongoRepository<Tariff, String>{
	Flux<Tariff> findByUtilityId(String utilityId);
	Mono<Tariff> findByName(String name);
}
