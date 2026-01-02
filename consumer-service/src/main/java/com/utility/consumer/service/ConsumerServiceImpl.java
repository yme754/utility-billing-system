package com.utility.consumer.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ConsumerServiceImpl implements ConsumerService{
	private final ConsumerRepository consumerRepo;
	private final ConnectionRepository connectionRepo;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	
	@Override
	public Mono<ConsumerDTO> createProfile(ConsumerDTO dto) {
		return consumerRepo.findByUserId(dto.getUserId())
				.flatMap(existing-> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists")))
				.switchIfEmpty(Mono.defer(() -> {
					Consumer consumer = Consumer.builder()
							.userId(dto.getUserId())
							.firstName(dto.getFirstName())
							.lastName(dto.getLastName())
							.email(dto.getEmail())
							.phoneNumber(dto.getPhoneNumber())
							.address(dto.getAddress())
							.active(true).build();
					return consumerRepo.save(consumer);
				}))
				.cast(Consumer.class)
				.map(this::mapToConsumerDTO);
	}
	
	@Override
	public Mono<ConsumerDTO> getProfile(String userId) {
        return consumerRepo.findByUserId(userId)
                .map(this::mapToConsumerDTO)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")));
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
                            .status("PENDING")
                            .connectionDate(LocalDate.now())
                            .build();
                    return connectionRepo.save(connection);
                })
                .map(this::mapToConnectionDTO);
    }
	
	@Override
	public Mono<ConnectionDTO> approveConnection(String connectionId, String meterNumber) {
		return connectionRepo.findById(connectionId)
	            .flatMap(connection -> {	                
	                connection.setStatus("ACTIVE");
	                connection.setMeterNumber(meterNumber);	                
	                return connectionRepo.save(connection).doOnSuccess(savedConn -> {
	                    EmailRequest email = new EmailRequest(
	                        "yxsh2999@gmail.com",
	                        "Connection Approved",
	                        "Your connection for " + savedConn.getUtilityType() 
	                        + " is now ACTIVE. Meter: " + savedConn.getMeterNumber()
	                    );
	                    kafkaTemplate.send("notification-topic", email);
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
                .flatMap(connection -> {
                    return consumerRepo.findById(connection.getConsumerId())
                            .map(consumer -> {
                                ConnectionDTO dto = mapToConnectionDTO(connection);
                                dto.setConsumerName(consumer.getFirstName() + " " + consumer.getLastName());
                                return dto;
                            })
                            .defaultIfEmpty(mapToConnectionDTO(connection));
                });
    }

    @Override
    public Flux<ConnectionDTO> getAllConnections() {
        return connectionRepo.findAll()
                .flatMap(connection -> 
                    consumerRepo.findById(connection.getConsumerId())
                        .map(consumer -> {
                            ConnectionDTO dto = mapToConnectionDTO(connection);
                            dto.setConsumerName(consumer.getFirstName() + " " + consumer.getLastName());
                            return dto;
                        })
                        .defaultIfEmpty(mapToConnectionDTO(connection))
                );
    }

    private ConsumerDTO mapToConsumerDTO(Consumer c) {
        return ConsumerDTO.builder()
                .id(c.getId()).userId(c.getUserId())
                .firstName(c.getFirstName()).lastName(c.getLastName())
                .email(c.getEmail()).address(c.getAddress())
                .phoneNumber(c.getPhoneNumber())
                .build();
    }

    private ConnectionDTO mapToConnectionDTO(Connection c) {
        return ConnectionDTO.builder()
                .id(c.getId()).consumerId(c.getConsumerId())
                .utilityType(c.getUtilityType())
                .meterNumber(c.getMeterNumber())
                .tariffCategory(c.getTariffCategory())
                .status(c.getStatus())
                .connectionDate(c.getConnectionDate())
                .build();
    }
}
