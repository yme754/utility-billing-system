package com.utility.consumer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import com.utility.consumer.dto.ConnectionApprovalDTO;
import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.dto.ConsumerDTO;
import com.utility.consumer.entity.Connection;
import com.utility.consumer.service.ConnectionService;
import com.utility.consumer.service.ConsumerService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ConsumerControllerTest {

    @Mock
    private ConsumerService consumerService;

    @Mock
    private ConnectionService connectionService;

    private ConsumerController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new ConsumerController(consumerService, connectionService);
    }

    @Test
    void createProfile_returnsConsumerDTO() {
        ConsumerDTO dto = new ConsumerDTO();
        when(consumerService.createProfile(dto)).thenReturn(Mono.just(dto));

        StepVerifier.create(controller.createProfile(dto))
                .expectNext(ResponseEntity.ok(dto))
                .verifyComplete();
    }

    @Test
    void getProfile_found_returnsConsumerDTO() {
        ConsumerDTO dto = new ConsumerDTO();
        when(consumerService.getProfile("u1")).thenReturn(Mono.just(dto));

        StepVerifier.create(controller.getProfile("u1"))
                .expectNext(ResponseEntity.ok(dto))
                .verifyComplete();
    }

    @Test
    void getProfile_notFound_returnsNotFound() {
        when(consumerService.getProfile("u1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.getProfile("u1"))
                .expectNext(ResponseEntity.notFound().build())
                .verifyComplete();
    }

    @Test
    void requestConnection_returnsConnection() {
        ConnectionDTO dto = new ConnectionDTO();
        dto.setConsumerId("c1");
        dto.setUtilityType("ELECTRICITY");
        dto.setTariffCategory("Residential");

        Connection conn = Connection.builder().consumerId("c1").utilityType("ELECTRICITY").tariffCategory("Residential").status("PENDING").build();
        when(connectionService.requestConnection(any(Connection.class))).thenReturn(Mono.just(conn));

        StepVerifier.create(controller.requestConnection(dto))
                .expectNext(ResponseEntity.ok(conn))
                .verifyComplete();
    }

    @Test
    void getMyConnections_returnsFlux() {
    	ConsumerDTO dto = new ConsumerDTO();
        when(consumerService.getAllConsumers()).thenReturn(Flux.just(dto));
        Mono<ResponseEntity<Flux<ConsumerDTO>>> result = controller.getAllConsumers();
        StepVerifier.create(result.flatMapMany(HttpEntity::getBody))
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    void approveConnection_returnsConnection() {
        ConnectionApprovalDTO approval = new ConnectionApprovalDTO();
        approval.setMeterNumber("m123");
        Connection conn = Connection.builder().id("c1").meterNumber("m123").build();
        when(connectionService.approveConnection("c1", "m123")).thenReturn(Mono.just(conn));

        StepVerifier.create(controller.approveConnection("c1", approval))
                .expectNext(ResponseEntity.ok(conn))
                .verifyComplete();
    }

    @Test
    void updateProfile_returnsConsumerDTO() {
        ConsumerDTO dto = new ConsumerDTO();
        when(consumerService.updateProfile(dto)).thenReturn(Mono.just(dto));

        StepVerifier.create(controller.updateProfile(dto))
                .expectNext(ResponseEntity.ok(dto))
                .verifyComplete();
    }

    @Test
    void updateConnectionStatus_returnsConnection() {
        Connection conn = Connection.builder().id("c1").status("APPROVED").build();
        when(connectionService.updateConnectionStatus("c1", "APPROVED")).thenReturn(Mono.just(conn));

        StepVerifier.create(controller.updateConnectionStatus("c1", Map.of("status", "APPROVED")))
                .expectNext(ResponseEntity.ok(conn))
                .verifyComplete();
    }

    @Test
    void getAllConsumers_returnsFlux() {
        ConsumerDTO dto = new ConsumerDTO();
        when(consumerService.getAllConsumers()).thenReturn(Flux.just(dto));

        Mono<ResponseEntity<Flux<ConsumerDTO>>> result = controller.getAllConsumers();

        StepVerifier.create(result.flatMapMany(resp -> resp.getBody()))
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    void getConsumerCount_returnsCount() {
        ConsumerDTO dto = new ConsumerDTO();
        when(consumerService.getAllConsumers()).thenReturn(Flux.just(dto));

        StepVerifier.create(controller.getConsumerCount())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void getAllPendingConnections_returnsFlux() {
        ConnectionDTO dto = new ConnectionDTO();
        when(connectionService.getPendingConnections()).thenReturn(Flux.just(dto));

        StepVerifier.create(controller.getAllPendingConnections())
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    void getConnectionById_returnsConnectionDTO() {
        ConnectionDTO dto = new ConnectionDTO();
        when(consumerService.getConnectionById("c1")).thenReturn(Mono.just(dto));

        StepVerifier.create(controller.getConnectionById("c1"))
                .expectNext(ResponseEntity.ok(dto))
                .verifyComplete();
    }

    @Test
    void getAllConnections_returnsFlux() {
        ConnectionDTO dto = new ConnectionDTO();
        when(consumerService.getAllConnections()).thenReturn(Flux.just(dto));

        StepVerifier.create(controller.getAllConnections())
                .expectNext(dto)
                .verifyComplete();
    }
}