package com.utility.consumer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.utility.consumer.entity.TariffPlan;
import com.utility.consumer.repository.TariffPlanRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TariffControllerTest {
	
    @Mock
    private TariffPlanRepository tariffRepo;

    private TariffController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new TariffController(tariffRepo);
    }

    @Test
    void createTariff_returnsTariffPlan() {
        TariffPlan plan = new TariffPlan();
        when(tariffRepo.save(plan)).thenReturn(Mono.just(plan));

        StepVerifier.create(controller.createTariff(plan))
                .expectNext(ResponseEntity.ok(plan))
                .verifyComplete();
    }

    @Test
    void getTariffs_withType_returnsFiltered() {
        TariffPlan plan = new TariffPlan();
        when(tariffRepo.findByUtilityType("ELECTRICITY")).thenReturn(Flux.just(plan));

        StepVerifier.create(controller.getTariffs("ELECTRICITY"))
                .expectNext(plan)
                .verifyComplete();
    }

    @Test
    void getTariffs_withoutType_returnsAll() {
        TariffPlan plan = new TariffPlan();
        when(tariffRepo.findAll()).thenReturn(Flux.just(plan));

        StepVerifier.create(controller.getTariffs(null))
                .expectNext(plan)
                .verifyComplete();
    }

    @Test
    void updateTariff_found_updatesAndReturns() {
        TariffPlan plan = new TariffPlan();
        plan.setPlanName("Residential");
        when(tariffRepo.findById("t1")).thenReturn(Mono.just(new TariffPlan()));
        when(tariffRepo.save(any(TariffPlan.class))).thenReturn(Mono.just(plan));

        StepVerifier.create(controller.updateTariff("t1", plan))
                .expectNext(ResponseEntity.ok(plan))
                .verifyComplete();
    }

    @Test
    void updateTariff_notFound_returnsNotFound() {
        TariffPlan plan = new TariffPlan();
        when(tariffRepo.findById("t1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.updateTariff("t1", plan))
                .expectNext(ResponseEntity.notFound().build())
                .verifyComplete();
    }

    @Test
    void deleteTariff_returnsOk() {
        when(tariffRepo.deleteById("t1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteTariff("t1"))
                .expectNext(ResponseEntity.ok().build())
                .verifyComplete();
    }
}