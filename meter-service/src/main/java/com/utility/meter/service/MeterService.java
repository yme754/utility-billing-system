package com.utility.meter.service;

import com.utility.meter.entity.MeterReading;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MeterService {	
	Mono<MeterReading> addReading(MeterReading meterReading);
	Flux<MeterReading> getReadingsByMeter(String meterId);
}
