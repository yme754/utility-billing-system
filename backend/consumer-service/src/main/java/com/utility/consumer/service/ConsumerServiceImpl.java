package com.utility.consumer.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.dto.ConsumerDTO;
import com.utility.consumer.dto.EmailRequest;
import com.utility.consumer.entity.Connection;
import com.utility.consumer.entity.Consumer;
import com.utility.consumer.repository.ConnectionRepository;
import com.utility.consumer.repository.ConsumerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerServiceImpl implements ConsumerService{
	private final ConsumerRepository consumerRepo;
    private final ConnectionRepository connectionRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String NOTIFICATION_TOPIC = "notification-topic";
    private static final String ADMIN_EMAIL = "yxsh2999@gmail.com";

    @Override
    public Mono<ConsumerDTO> createProfile(ConsumerDTO dto) {
        return consumerRepo.findByUserId(dto.getUserId())
                .flatMap(existing -> {
                    updateEntityWithDto(existing, dto);
                    return consumerRepo.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> consumerRepo.save(mapToEntity(dto))))
                .map(this::mapToConsumerDTO);
    }

    @Override
    public Mono<ConsumerDTO> getProfile(String userId) {
        return consumerRepo.findByUserId(userId)
                .map(this::mapToConsumerDTO)
                .switchIfEmpty(Mono.defer(() -> autoCreateProfile(userId)));
    }
    
    private Mono<ConsumerDTO> autoCreateProfile(String userId) {
    	log.warn("Auto-creating empty profile for userId: {}. Registration Sync must have failed!", userId);
        Consumer newConsumer = Consumer.builder()
                .userId(userId)
                .firstName("Update")
                .lastName("Profile")
                .phoneNumber("")
                .email("")
                .address("")
                .active(true)
                .profileImageUrl("")
                .build();
        return consumerRepo.save(newConsumer)
                .map(this::mapToConsumerDTO);
    }

    @Override
    public Mono<ConsumerDTO> updateProfile(ConsumerDTO dto) {
        return consumerRepo.findByUserId(dto.getUserId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")))
                .flatMap(existing -> {
                    existing.setFirstName(dto.getFirstName());
                    existing.setLastName(dto.getLastName());
                    existing.setPhoneNumber(dto.getPhoneNumber());
                    existing.setAddress(dto.getAddress());
                    existing.setEmail(dto.getEmail());
                    existing.setActive(dto.isActive());
                    if (dto.getProfileImageUrl() != null) {
                        existing.setProfileImageUrl(dto.getProfileImageUrl());
                    }
                    return consumerRepo.save(existing);
                })
                .map(this::mapToConsumerDTO);
    }

    @Override
    public Mono<ConnectionDTO> requestConnection(ConnectionDTO dto) {
    	return consumerRepo.findById(dto.getConsumerId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Consumer not found")))
                .flatMap(consumer -> {
                    Connection connection = Connection.builder()
                            .consumerId(consumer.getId())
                            .utilityType(dto.getUtilityType())
                            .tariffCategory(dto.getTariffCategory())
                            .status(STATUS_PENDING)
                            .connectionDate(LocalDate.now())
                            .build();
                    return connectionRepo.save(connection);
                })
                .map(this::mapToConnectionDTO);
    }

    @Override
    public Mono<ConnectionDTO> approveConnection(String connectionId, String meterNumber) {
    	return connectionRepo.findById(connectionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found")))
                .flatMap(connection -> {
                    connection.setStatus(STATUS_ACTIVE);
                    connection.setMeterNumber(meterNumber);
                    return connectionRepo.save(connection).doOnSuccess(savedConn -> {
                        try {
                            EmailRequest email = new EmailRequest(
                                ADMIN_EMAIL,
                                "Connection Approved",
                                "Your connection for " + savedConn.getUtilityType() 
                                + " is now " + STATUS_ACTIVE + ". Meter: " + savedConn.getMeterNumber()
                            );
                            kafkaTemplate.send(NOTIFICATION_TOPIC, email);
                        } catch (Exception e) {
                            log.error("Failed to send email: {}", e.getMessage());
                        }
                    });
                })
                .map(this::mapToConnectionDTO);
    }

    @Override
    public Flux<ConnectionDTO> getConnectionsByConsumer(String consumerId) {
    	return connectionRepo.findAllByConsumerId(consumerId)
                .map(this::mapToConnectionDTO);
    }

    @Override
    public Flux<ConsumerDTO> getAllConsumers() {
    	return consumerRepo.findAll()
                .map(this::mapToConsumerDTO);
    }

    @Override
    public Mono<ConnectionDTO> getConnectionById(String connectionId) {
        return connectionRepo.findById(connectionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found")))
                .flatMap(connection -> consumerRepo.findById(connection.getConsumerId())
                        .map(consumer -> {
                            ConnectionDTO dto = mapToConnectionDTO(connection);
                            dto.setConsumerName(consumer.getFirstName() + " " + consumer.getLastName());
                            return dto;
                        })
                        .defaultIfEmpty(mapToConnectionDTO(connection)));
    }

    @Override
    public Flux<ConnectionDTO> getAllConnections() {
        return connectionRepo.findAll()
                .flatMap(connection -> consumerRepo.findById(connection.getConsumerId())
                        .map(consumer -> {
                            ConnectionDTO dto = mapToConnectionDTO(connection);
                            dto.setConsumerName(consumer.getFirstName() + " " + consumer.getLastName());
                            return dto;
                        })
                        .defaultIfEmpty(mapToConnectionDTO(connection)));
    }

    private ConnectionDTO mapToConnectionDTO(Connection c) {
        return ConnectionDTO.builder()
                .id(c.getId())
                .consumerId(c.getConsumerId())
                .utilityType(c.getUtilityType())
                .meterNumber(c.getMeterNumber())
                .tariffCategory(c.getTariffCategory())
                .status(c.getStatus())
                .connectionDate(c.getConnectionDate())
                .build();
    }
    
    private void updateEntityWithDto(Consumer existing, ConsumerDTO dto) {
        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setPhoneNumber(dto.getPhoneNumber());
        existing.setAddress(dto.getAddress());
        existing.setEmail(dto.getEmail());
        existing.setActive(dto.isActive());
        if (dto.getProfileImageUrl() != null) {
            existing.setProfileImageUrl(dto.getProfileImageUrl());
        }
    }

    private ConsumerDTO mapToConsumerDTO(Consumer c) {
        return ConsumerDTO.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .phoneNumber(c.getPhoneNumber())
                .address(c.getAddress())
                .active(c.isActive()) 
                .profileImageUrl(c.getProfileImageUrl())
                .build();
    }

    private Consumer mapToEntity(ConsumerDTO d) {
        return Consumer.builder()
                .userId(d.getUserId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .email(d.getEmail())
                .phoneNumber(d.getPhoneNumber())
                .address(d.getAddress())
                .active(d.isActive())
                .profileImageUrl(d.getProfileImageUrl())
                .build();
    }
}