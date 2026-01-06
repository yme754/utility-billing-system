package com.utility.consumer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.utility.consumer.entity.Connection;
import com.utility.consumer.entity.Consumer;
import com.utility.consumer.repository.ConnectionRepository;
import com.utility.consumer.repository.ConsumerRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ConnectionServiceImplTest {

    @Mock
    private ConnectionRepository connectionRepo;

    @Mock
    private ConsumerRepository consumerRepo;

    private ConnectionServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new ConnectionServiceImpl(connectionRepo, consumerRepo);
    }

    @Test
    void approveConnection_happyPath() {
        Connection conn = Connection.builder().id("c1").status("PENDING").build();
        when(connectionRepo.findById("c1")).thenReturn(Mono.just(conn));
        when(connectionRepo.save(any(Connection.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.approveConnection("c1", "m123"))
                .expectNextMatches(saved -> "ACTIVE".equals(saved.getStatus()) && "m123".equals(saved.getMeterNumber()))
                .verifyComplete();
    }

    @Test
    void approveConnection_alreadyActive_throwsError() {
        Connection conn = Connection.builder().id("c1").status("ACTIVE").build();
        when(connectionRepo.findById("c1")).thenReturn(Mono.just(conn));

        StepVerifier.create(service.approveConnection("c1", "m123"))
                .expectErrorMatches(err -> err instanceof ResponseStatusException &&
                        ((ResponseStatusException) err).getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void approveConnection_notFound_throwsError() {
        when(connectionRepo.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(service.approveConnection("missing", "m123"))
                .expectErrorMatches(err -> err instanceof ResponseStatusException &&
                        ((ResponseStatusException) err).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getPendingConnections_withConsumerName() {
        Connection conn = Connection.builder().id("c1").consumerId("u1").status("PENDING").build();
        Consumer consumer = Consumer.builder().id("u1").firstName("John").lastName("Doe").build();

        when(connectionRepo.findByStatusNot("ACTIVE")).thenReturn(Flux.just(conn));
        when(consumerRepo.findById("u1")).thenReturn(Mono.just(consumer));

        StepVerifier.create(service.getPendingConnections())
                .expectNextMatches(dto -> dto.getConsumerName().equals("John Doe"))
                .verifyComplete();
    }

    @Test
    void getPendingConnections_withoutConsumerName() {
        Connection conn = Connection.builder().id("c1").consumerId("u1").status("PENDING").build();

        when(connectionRepo.findByStatusNot("ACTIVE")).thenReturn(Flux.just(conn));
        when(consumerRepo.findById("u1")).thenReturn(Mono.empty());

        StepVerifier.create(service.getPendingConnections())
                .expectNextMatches(dto -> dto.getConsumerName() == null)
                .verifyComplete();
    }

    @Test
    void requestConnection_setsPendingAndDate() {
        Connection conn = Connection.builder().id("c1").status(null).connectionDate(null).build();
        when(connectionRepo.save(any(Connection.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.requestConnection(conn))
                .expectNextMatches(saved -> "PENDING".equals(saved.getStatus()) && saved.getConnectionDate() != null)
                .verifyComplete();
    }

    @Test
    void getMyConnections_returnsFlux() {
        Connection conn = Connection.builder().id("c1").consumerId("u1").build();
        when(connectionRepo.findByConsumerId("u1")).thenReturn(Flux.just(conn));

        StepVerifier.create(service.getMyConnections("u1"))
                .expectNext(conn)
                .verifyComplete();
    }

    @Test
    void updateConnectionStatus_happyPath() {
        Connection conn = Connection.builder().id("c1").status("PENDING").build();
        when(connectionRepo.findById("c1")).thenReturn(Mono.just(conn));
        when(connectionRepo.save(any(Connection.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.updateConnectionStatus("c1", "APPROVED"))
                .expectNextMatches(saved -> "APPROVED".equals(saved.getStatus()))
                .verifyComplete();
    }

    @Test
    void updateConnectionStatus_notFound_throwsError() {
        when(connectionRepo.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(service.updateConnectionStatus("missing", "APPROVED"))
                .expectErrorMatches(err -> err instanceof ResponseStatusException &&
                        ((ResponseStatusException) err).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }
}