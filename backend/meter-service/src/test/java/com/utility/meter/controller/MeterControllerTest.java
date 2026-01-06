package com.utility.meter.controller;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.utility.meter.entity.MeterReading;
import com.utility.meter.service.MeterService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MeterControllerTest {
	
    @Mock
    private MeterService service;

    private MeterController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new MeterController(service);
    }

    @Test
    void addReading_returnsResponseEntity() {
        MeterReading reading = new MeterReading();
        when(service.addReading(reading)).thenReturn(Mono.just(reading));

        StepVerifier.create(controller.addReading(reading))
                .expectNext(ResponseEntity.ok(reading))
                .verifyComplete();
    }

    @Test
    void getHistory_returnsFlux() {
        MeterReading reading = new MeterReading();
        when(service.getReadingsByMeter("m1")).thenReturn(Flux.just(reading));

        StepVerifier.create(controller.getHistory("m1"))
                .expectNext(reading)
                .verifyComplete();
    }

    @Test
    void getCurrentMonthReadings_returnsFlux() {
        MeterReading reading = new MeterReading();
        when(service.getReadingsForCurrentMonth()).thenReturn(Flux.just(reading));

        StepVerifier.create(controller.getCurrentMonthReadings())
                .expectNext(reading)
                .verifyComplete();
    }
}