package com.utility.consumer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utility.consumer.dto.ConsumerDTO;
import com.utility.consumer.service.ConsumerService;

import lombok.RequiredArgsConstructor;
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
}
