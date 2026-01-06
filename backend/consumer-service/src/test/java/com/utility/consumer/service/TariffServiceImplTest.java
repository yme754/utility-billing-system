package com.utility.consumer.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.utility.consumer.entity.TariffPlan;
import com.utility.consumer.repository.TariffPlanRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TariffServiceImplTest {

    @Mock
    private TariffPlanRepository tariffRepo;

    private TariffServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new TariffServiceImpl(tariffRepo);
    }

    @Test
    void addPlan_savesAndReturns() {
        TariffPlan plan = new TariffPlan();
        when(tariffRepo.save(plan)).thenReturn(Mono.just(plan));

        StepVerifier.create(service.addPlan(plan))
                .expectNext(plan)
                .verifyComplete();
    }

    @Test
    void getAllPlans_returnsFlux() {
        TariffPlan plan = new TariffPlan();
        when(tariffRepo.findAll()).thenReturn(Flux.just(plan));

        StepVerifier.create(service.getAllPlans())
                .expectNext(plan)
                .verifyComplete();
    }

    @Test
    void getPlansByType_returnsFilteredFlux() {
        TariffPlan plan = new TariffPlan();
        when(tariffRepo.findByUtilityType("ELECTRICITY")).thenReturn(Flux.just(plan));

        StepVerifier.create(service.getPlansByType("ELECTRICITY"))
                .expectNext(plan)
                .verifyComplete();
    }
}