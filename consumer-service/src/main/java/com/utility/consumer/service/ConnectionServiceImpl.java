package com.utility.consumer.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.consumer.entity.Connection;
import com.utility.consumer.repository.ConnectionRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService{
	private final ConnectionRepository connectionRepo;
    @Override
    public Mono<Connection> approveConnection(String connectionId, String meterNumber) {
        return connectionRepo.findById(connectionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection Request not found")))
                .flatMap(connection -> {
                    if ("ACTIVE".equals(connection.getStatus()))
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection is already active"));
                    connection.setMeterNumber(meterNumber);
                    connection.setStatus("ACTIVE");
                    connection.setConnectionDate(LocalDate.now());
                    return connectionRepo.save(connection);
                });
    }
}
