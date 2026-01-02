package com.utility.consumer.service;

import org.springframework.stereotype.Service;

import com.utility.consumer.entity.TariffPlan;
import com.utility.consumer.repository.TariffPlanRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TariffServiceImpl implements TariffService{
	private final TariffPlanRepository tariffPlanRepo;

	@Override
    public Mono<TariffPlan> addPlan(TariffPlan plan) {
        return tariffPlanRepo.save(plan);
    }

	@Override
    public Flux<TariffPlan> getAllPlans() {
        return tariffPlanRepo.findAll();
    }
    
	@Override
    public Flux<TariffPlan> getPlansByType(String type) {
        return tariffPlanRepo.findByUtilityType(type);
    }
}
