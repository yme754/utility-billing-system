package com.utility.consumer.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.utility.consumer.entity.TariffPlan;

import reactor.core.publisher.Flux;

public interface TariffPlanRepository extends ReactiveMongoRepository<TariffPlan, String>{
	Flux<TariffPlan> findByUtilityType(String utilityType);
}
