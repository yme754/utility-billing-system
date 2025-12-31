package com.utility.consumer.service;

import com.utility.consumer.entity.Connection;

import reactor.core.publisher.Mono;

public interface ConnectionService {
	Mono<Connection> approveConnection(String connectionId, String meterNumber);
}
