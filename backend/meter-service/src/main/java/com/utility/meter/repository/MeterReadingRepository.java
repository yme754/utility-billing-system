package com.utility.meter.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.utility.meter.entity.MeterReading;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MeterReadingRepository extends ReactiveMongoRepository<MeterReading, String>{
	Flux<MeterReading> findByMeterId(String meterId);
	Mono<MeterReading> findTopByMeterIdOrderByDateDesc(String meterId);
}
