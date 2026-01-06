package com.utility.consumer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.dto.ConsumerDTO;
import com.utility.consumer.entity.Connection;
import com.utility.consumer.entity.Consumer;
import com.utility.consumer.repository.ConnectionRepository;
import com.utility.consumer.repository.ConsumerRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ConsumerServiceImplTest {

    @Mock
    private ConsumerRepository consumerRepo;

    @Mock
    private ConnectionRepository connectionRepo;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ConsumerServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new ConsumerServiceImpl(consumerRepo, connectionRepo, kafkaTemplate);
    }

    @Test
    void createProfile_conflictThrowsError() {
        ConsumerDTO dto = ConsumerDTO.builder().userId("u1").build();
        when(consumerRepo.findByUserId("u1")).thenReturn(Mono.just(new Consumer()));
        StepVerifier.create(service.createProfile(dto))
                .expectErrorMatches(err -> err instanceof ResponseStatusException &&
                        ((ResponseStatusException) err).getStatusCode() == HttpStatus.CONFLICT)
                .verify();
    }

    @Test
    void createProfile_happyPath() {
        ConsumerDTO dto = ConsumerDTO.builder().userId("u1").firstName("John").build();
        when(consumerRepo.findByUserId("u1")).thenReturn(Mono.empty());
        when(consumerRepo.save(any(Consumer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        StepVerifier.create(service.createProfile(dto))
                .expectNextMatches(saved -> saved.getUserId().equals("u1"))
                .verifyComplete();
    }

    @Test
    void updateProfile_notFoundThrowsError() {
        ConsumerDTO dto = ConsumerDTO.builder().userId("u1").build();
        when(consumerRepo.findByUserId("u1")).thenReturn(Mono.empty());
        StepVerifier.create(service.updateProfile(dto))
                .expectErrorMatches(err -> err instanceof ResponseStatusException &&
                        ((ResponseStatusException) err).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void requestConnection_consumerNotFoundThrowsError() {
        ConnectionDTO dto = ConnectionDTO.builder().consumerId("c1").build();
        when(consumerRepo.findById("c1")).thenReturn(Mono.empty());
        StepVerifier.create(service.requestConnection(dto))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void approveConnection_happyPath_sendsKafka() {
        Connection conn = Connection.builder().id("c1").status("PENDING").utilityType("ELECTRICITY").build();
        when(connectionRepo.findById("c1")).thenReturn(Mono.just(conn));
        when(connectionRepo.save(any(Connection.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        StepVerifier.create(service.approveConnection("c1", "m123"))
                .expectNextMatches(dto -> "ACTIVE".equals(dto.getStatus()) && "m123".equals(dto.getMeterNumber()))
                .verifyComplete();
        verify(kafkaTemplate).send(eq("notification-topic"), any());
    }

    @Test
    void approveConnection_notFoundThrowsError() {
        when(connectionRepo.findById("missing")).thenReturn(Mono.empty());
        StepVerifier.create(service.approveConnection("missing", "m123"))
                .expectErrorMatches(err -> err instanceof ResponseStatusException &&
                        ((ResponseStatusException) err).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getAllConsumers_returnsFlux() {
        Consumer consumer = Consumer.builder().id("c1").userId("u1").firstName("John").build();
        when(consumerRepo.findAll()).thenReturn(Flux.just(consumer));
        StepVerifier.create(service.getAllConsumers())
                .expectNextMatches(dto -> dto.getUserId().equals("u1"))
                .verifyComplete();
    }

    @Test
    void getConnectionById_withConsumerName() {
        Connection conn = Connection.builder().id("c1").consumerId("u1").status("ACTIVE").build();
        Consumer consumer = Consumer.builder().id("u1").firstName("John").lastName("Doe").build();
        when(connectionRepo.findById("c1")).thenReturn(Mono.just(conn));
        when(consumerRepo.findById("u1")).thenReturn(Mono.just(consumer));
        StepVerifier.create(service.getConnectionById("c1"))
                .expectNextMatches(dto -> "John Doe".equals(dto.getConsumerName()))
                .verifyComplete();
    }

    @Test
    void getConnectionById_notFoundThrowsError() {
        when(connectionRepo.findById("missing")).thenReturn(Mono.empty());
        StepVerifier.create(service.getConnectionById("missing"))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void getAllConnections_returnsFluxWithConsumerName() {
        Connection conn = Connection.builder().id("c1").consumerId("u1").status("ACTIVE").build();
        Consumer consumer = Consumer.builder().id("u1").firstName("Jane").lastName("Smith").build();
        when(connectionRepo.findAll()).thenReturn(Flux.just(conn));
        when(consumerRepo.findById("u1")).thenReturn(Mono.just(consumer));
        StepVerifier.create(service.getAllConnections())
                .expectNextMatches(dto -> "Jane Smith".equals(dto.getConsumerName()))
                .verifyComplete();
    }
    
    @Test
    void requestConnection_happyPath_savesConnection() {
        Consumer consumer = Consumer.builder().id("c1").build();
        ConnectionDTO dto = ConnectionDTO.builder()
                .consumerId("c1")
                .utilityType("ELECTRICITY")
                .tariffCategory("Residential")
                .build();
        when(consumerRepo.findById("c1")).thenReturn(Mono.just(consumer));
        when(connectionRepo.save(any(Connection.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        StepVerifier.create(service.requestConnection(dto))
                .expectNextMatches(conn -> "PENDING".equals(conn.getStatus()) && conn.getConsumerId().equals("c1"))
                .verifyComplete();
    }

    @Test
    void updateProfile_withImageUrl_persistsImage() {
        Consumer existing = Consumer.builder()
                .id("c1")
                .userId("u1")
                .firstName("Old")
                .lastName("Name")
                .build();
        ConsumerDTO dto = ConsumerDTO.builder()
                .userId("u1")
                .firstName("New")
                .lastName("Name")
                .profileImageUrl("http://image.png")
                .build();
        when(consumerRepo.findByUserId("u1")).thenReturn(Mono.just(existing));
        when(consumerRepo.save(any(Consumer.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        StepVerifier.create(service.updateProfile(dto))
                .expectNextMatches(updated -> "http://image.png".equals(updated.getProfileImageUrl()))
                .verifyComplete();
    }

    @Test
    void approveConnection_sendsKafkaNotification() {
        Connection conn = Connection.builder()
                .id("c1")
                .status("PENDING")
                .utilityType("ELECTRICITY")
                .build();
        when(connectionRepo.findById("c1")).thenReturn(Mono.just(conn));
        when(connectionRepo.save(any(Connection.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        StepVerifier.create(service.approveConnection("c1", "m123"))
                .expectNextMatches(dto -> "ACTIVE".equals(dto.getStatus()) && "m123".equals(dto.getMeterNumber()))
                .verifyComplete();
        verify(kafkaTemplate, times(1)).send(eq("notification-topic"), any());
    }

}