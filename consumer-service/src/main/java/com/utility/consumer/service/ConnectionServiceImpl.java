package com.utility.consumer.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.entity.Connection;
import com.utility.consumer.repository.ConnectionRepository;
import com.utility.consumer.repository.ConsumerRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService{
	private final ConnectionRepository connectionRepo;
	private final ConsumerRepository consumerRepo;
	
	private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
	
    @Override
    public Mono<Connection> approveConnection(String connectionId, String meterNumber) {
        return connectionRepo.findById(connectionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection Request not found")))
                .flatMap(connection -> {
                    if (STATUS_ACTIVE.equals(connection.getStatus()))
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection is already active"));
                    connection.setMeterNumber(meterNumber);
                    connection.setStatus(STATUS_ACTIVE);
                    connection.setConnectionDate(LocalDate.now());
                    return connectionRepo.save(connection);
                });
    }
    
    @Override
    public Flux<ConnectionDTO> getPendingConnections() {
    	return connectionRepo.findByStatusNot(STATUS_ACTIVE)
                .flatMap(conn -> 
                    consumerRepo.findById(conn.getConsumerId()) 
                        .map(consumer -> {
                            ConnectionDTO dto = mapToDTO(conn);
                            dto.setConsumerName(consumer.getFirstName() + " " + consumer.getLastName());
                            return dto;
                        })
                        .defaultIfEmpty(mapToDTO(conn)) 
                );
    }
    
    @Override
    public Mono<Connection> requestConnection(Connection connection) {
        connection.setStatus(STATUS_PENDING);
        if (connection.getConnectionDate() == null) connection.setConnectionDate(LocalDate.now());     
        return connectionRepo.save(connection);
    }
    
    @Override
    public Flux<Connection> getMyConnections(String consumerId) {
        return connectionRepo.findByConsumerId(consumerId);
    }
    
    @Override
    public Mono<Connection> updateConnectionStatus(String id, String newStatus) {
        return connectionRepo.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found")))
                .flatMap(connection -> {
                    connection.setStatus(newStatus);
                    return connectionRepo.save(connection);
                });
    }
    
    private ConnectionDTO mapToDTO(Connection connection) {
        return ConnectionDTO.builder()
                .id(connection.getId())
                .consumerId(connection.getConsumerId())
                .utilityType(connection.getUtilityType())
                .tariffCategory(connection.getTariffCategory())
                .meterNumber(connection.getMeterNumber())
                .connectionDate(connection.getConnectionDate())
                .status(connection.getStatus())
                .build();
    }
}
