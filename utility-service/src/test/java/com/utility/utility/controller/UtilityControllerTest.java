package com.utility.utility.controller;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.utility.utility.entity.Tariff;
import com.utility.utility.entity.Utility;
import com.utility.utility.service.UtilityService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UtilityControllerTest {

    @Mock
    private UtilityService service;

    private UtilityController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new UtilityController(service);
    }

    @Test
    void getAllUtilities_returnsFlux() {
        Utility u = new Utility();
        when(service.getAllUtilities()).thenReturn(Flux.just(u));

        StepVerifier.create(controller.getAllUtilities())
                .expectNext(u)
                .verifyComplete();
    }

    @Test
    void createTariff_returnsMono() {
        Tariff t = new Tariff();
        when(service.addTariff(t)).thenReturn(Mono.just(t));

        StepVerifier.create(controller.createTariff(t))
                .expectNext(t)
                .verifyComplete();
    }

    @Test
    void getTariffs_returnsFlux() {
        Tariff t = new Tariff();
        when(service.getTariffsByutility("ELECTRICITY")).thenReturn(Flux.just(t));

        StepVerifier.create(controller.getTariffs("ELECTRICITY"))
                .expectNext(t)
                .verifyComplete();
    }
}