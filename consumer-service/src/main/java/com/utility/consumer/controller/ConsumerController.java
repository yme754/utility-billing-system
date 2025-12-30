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

import com.utility.consumer.dto.ConnectionDTO;
import com.utility.consumer.dto.ConsumerDTO;
import com.utility.consumer.service.ConsumerService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/consumers")
@RequiredArgsConstructor
public class ConsumerController {
	private final ConsumerService consumerService;
	
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
	public Mono<ResponseEntity<ConnectionDTO>> requestConnection(@RequestBody ConnectionDTO dto) {
		return consumerService.requestConnection(dto).map(ResponseEntity::ok);
	}
	
	@GetMapping("/{consumerId}/connections")
	@PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER', 'CONSUMER')")
	public Flux<ConnectionDTO> getMyConnections(@PathVariable String consumerId) {
		return consumerService.getConnectionsByConsumer(consumerId);
	}
	
	@PutMapping("/connections/{id}/approve")
	@PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER')")
	public Mono<ResponseEntity<ConnectionDTO>> approveConnection(@PathVariable String id, @RequestParam String meterNumber) {
        return consumerService.approveConnection(id, meterNumber).map(ResponseEntity::ok);
    }
	
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER', 'ACCOUNTS_OFFICER')")
    public Mono<ResponseEntity<Flux<ConsumerDTO>>> getAllConsumers() {
        return Mono.just(ResponseEntity.ok(consumerService.getAllConsumers()));
    }
}
