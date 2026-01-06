package com.utility.consumer.service;

import com.utility.consumer.entity.TariffPlan;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TariffService {
	Mono<TariffPlan> addPlan(TariffPlan plan);
    Flux<TariffPlan> getAllPlans();    
    Flux<TariffPlan> getPlansByType(String type);
}
