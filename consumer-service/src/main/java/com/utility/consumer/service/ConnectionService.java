package com.utility.consumer.service;

import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.entity.Connection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConnectionService {
	Mono<Connection> approveConnection(String connectionId, String meterNumber);
	Flux<ConnectionDTO> getPendingConnections();
	Mono<Connection> requestConnection(Connection connection);
	Flux<Connection> getMyConnections(String consumerId);
	Mono<Connection> updateConnectionStatus(String id, String newStatus);
}
