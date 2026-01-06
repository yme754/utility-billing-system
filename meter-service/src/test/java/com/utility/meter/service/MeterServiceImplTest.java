package com.utility.meter.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.utility.meter.dto.MeterReadingEvent;
import com.utility.meter.entity.MeterReading;
import com.utility.meter.repository.MeterReadingRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MeterServiceImplTest {

    @Mock
    private MeterReadingRepository repo;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MeterServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new MeterServiceImpl(repo, kafkaTemplate);
    }

    @Test
    void addReading_setsDateAndSaves() {
        MeterReading reading = MeterReading.builder().meterId("m1").reading(10.0).build();
        when(repo.findTopByMeterIdOrderByDateDesc("m1")).thenReturn(Mono.empty());
        when(repo.save(reading)).thenReturn(Mono.just(reading));
        when(kafkaTemplate.send(eq("meter-reading-submitted"), any(MeterReadingEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(service.addReading(reading))
                .expectNext(reading)
                .verifyComplete();
    }

    @Test
    void addReading_lowerThanPrevious_throwsError() {
        MeterReading reading = MeterReading.builder().meterId("m1").reading(5.0).build();
        MeterReading previous = MeterReading.builder().reading(10.0).build();
        when(repo.findTopByMeterIdOrderByDateDesc("m1")).thenReturn(Mono.just(previous));

        StepVerifier.create(service.addReading(reading))
                .expectErrorMatches(err -> err instanceof ResponseStatusException &&
                        ((ResponseStatusException) err).getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void getReadingsForCurrentMonth_returnsFlux() {
        MeterReading r = new MeterReading();
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        when(repo.findByDateBetween(start, end)).thenReturn(Flux.just(r));

        StepVerifier.create(service.getReadingsForCurrentMonth())
                .expectNext(r)
                .verifyComplete();
    }

    @Test
    void getReadingsByMeter_returnsFlux() {
        MeterReading r = new MeterReading();
        when(repo.findByMeterId("m1")).thenReturn(Flux.just(r));

        StepVerifier.create(service.getReadingsByMeter("m1"))
                .expectNext(r)
                .verifyComplete();
    }
}