package com.utility.consumer.service;

import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.dto.ConsumerDTO;
import com.utility.consumer.entity.Connection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConsumerService {
	Mono<ConsumerDTO> createProfile(ConsumerDTO dto);
	Mono<ConsumerDTO> getProfile(String userId);
	Mono<ConnectionDTO> requestConnection(ConnectionDTO dto);
	Mono<ConnectionDTO> approveConnection(String connectionId, String meterNumber);
	Flux<ConnectionDTO> getConnectionsByConsumer(String consumerId);
	Flux<ConsumerDTO> getAllConsumers();
	Mono<ConnectionDTO> getConnectionById(String connectionId);
    Flux<ConnectionDTO> getAllConnections();
}
