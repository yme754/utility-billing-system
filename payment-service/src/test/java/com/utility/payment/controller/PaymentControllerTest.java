package com.utility.payment.controller;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.utility.payment.dto.PaymentRequest;
import com.utility.payment.entity.Payment;
import com.utility.payment.service.PaymentService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PaymentControllerTest {

    @Mock
    private PaymentService service;

    private PaymentController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new PaymentController(service);
    }

    @Test
    void makePayment_returnsResponseEntity() {
        PaymentRequest request = new PaymentRequest();
        Payment payment = new Payment();
        when(service.processPayment(request, "Bearer token")).thenReturn(Mono.just(payment));

        StepVerifier.create(controller.makePayment(request, "Bearer token"))
                .expectNext(ResponseEntity.ok(payment))
                .verifyComplete();
    }

    @Test
    void getPaymentHistory_returnsFluxWrappedInResponseEntity() {
        Payment payment = new Payment();
        when(service.getSuccessfulPayments()).thenReturn(Flux.just(payment));

        StepVerifier.create(controller.getPaymentHistory())
                .expectNextMatches(resp -> resp.getBody() != null)
                .verifyComplete();
    }

    @Test
    void getAccountStats_returnsResponseEntity() {
        Map<String, Object> stats = Map.of("totalPayments", 5);
        when(service.getAccountStats()).thenReturn(Mono.just(stats));

        StepVerifier.create(controller.getAccountStats())
                .expectNext(ResponseEntity.ok(stats))
                .verifyComplete();
    }
}