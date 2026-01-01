package com.utility.consumer.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utility.consumer.dto.ConnectionApprovalDTO;
import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.dto.ConsumerDTO;
import com.utility.consumer.entity.Connection;
import com.utility.consumer.service.ConnectionService;
import com.utility.consumer.service.ConsumerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/consumers")
@RequiredArgsConstructor
public class ConsumerController {
	private final ConsumerService consumerService;
    private final ConnectionService connectionService;
    
    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ConsumerDTO>> createProfile(@RequestBody ConsumerDTO consumerDTO, Principal principal) {
        return consumerService.createProfile(consumerDTO).map(ResponseEntity::ok);  
    }
    
    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER', 'ACCOUNTS_OFFICER', 'CONSUMER')")
    public Mono<ResponseEntity<ConsumerDTO>> getProfile(@PathVariable String userId) {
        return consumerService.getProfile(userId).map(ResponseEntity::ok);
    }
    
    @PostMapping("/connections")
    @PreAuthorize("hasRole('CONSUMER')")
    public Mono<ResponseEntity<ConnectionDTO>> requestConnection(@Valid @RequestBody ConnectionDTO dto) {
        return consumerService.requestConnection(dto).map(ResponseEntity::ok);
    }
    
    @GetMapping("/{consumerId}/connections")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER', 'CONSUMER')")
    public Flux<ConnectionDTO> getMyConnections(@PathVariable String consumerId) {
        return consumerService.getConnectionsByConsumer(consumerId);
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER')")
    public Mono<ResponseEntity<Connection>> approveConnection(@PathVariable String id, 
            @RequestBody ConnectionApprovalDTO approvalDto) { 
        return connectionService.approveConnection(id, approvalDto.getMeterNumber())
                .map(ResponseEntity::ok);
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER', 'ACCOUNTS_OFFICER')")
    public Mono<ResponseEntity<Flux<ConsumerDTO>>> getAllConsumers() {
        return Mono.just(ResponseEntity.ok(consumerService.getAllConsumers()));
    }
    
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER', 'ACCOUNTS_OFFICER')")
    public Mono<Long> getConsumerCount() {
        return consumerService.getAllConsumers().count();
    }
}
