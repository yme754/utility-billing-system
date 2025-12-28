package com.utility.consumer.controller;

import org.springframework.http.ResponseEntity;
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
	public Mono<ResponseEntity<ConsumerDTO>> createProfile(@RequestBody ConsumerDTO dto) {
		return consumerService.createProfile(dto).map(ResponseEntity::ok);
	}
	
	@GetMapping("/profile/{userId}")
	public Mono<ResponseEntity<ConsumerDTO>> getProfile(@PathVariable String userId) {
		return consumerService.getProfile(userId).map(ResponseEntity::ok);
	}
	
	@PostMapping("/connections")
	public Mono<ResponseEntity<ConnectionDTO>> requestConnection(@RequestBody ConnectionDTO dto) {
		return consumerService.requestConnection(dto).map(ResponseEntity::ok);
	}
	
	@GetMapping("/{consumerId}/connections")
	public Flux<ConnectionDTO> getMyConnections(@PathVariable String consumerId) {
		return consumerService.getConnectionsByConsumer(consumerId);
	}
	
	@PutMapping("/connections/{id}/approve")
	public Mono<ResponseEntity<ConnectionDTO>> approveConnection(@PathVariable String id, @RequestParam String meterNumber) {
		return consumerService.approveConnection(id, meterNumber).map(ResponseEntity::ok);
	}
}
