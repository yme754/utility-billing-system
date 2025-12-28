package com.utility.meter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utility.meter.entity.MeterReading;
import com.utility.meter.service.MeterService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/readings")
@RequiredArgsConstructor
public class MeterController {
	private final MeterService meterService;
	
	@PostMapping
	public Mono<ResponseEntity<MeterReading>> addReading(@RequestBody MeterReading meterReading) {
		return meterService.addReading(meterReading).map(ResponseEntity::ok);
	}
	
	@GetMapping("/{meterId}")
	public Flux<MeterReading> getHistory(@PathVariable String meterId) {
		return meterService.getReadingsByMeter(meterId);
	}
}
