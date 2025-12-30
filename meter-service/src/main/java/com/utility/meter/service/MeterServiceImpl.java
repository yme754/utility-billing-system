package com.utility.meter.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.meter.entity.MeterReading;
import com.utility.meter.repository.MeterReadingRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class MeterServiceImpl implements MeterService{
	
	private final MeterReadingRepository meterReadingRepo;
	
	@Override
	public Mono<MeterReading> addReading(MeterReading meterReading) {
		return meterReadingRepo.findTopByMeterIdOrderByDateDesc(meterReading.getMeterId())
				.defaultIfEmpty(MeterReading.builder().reading(0.0).build())
				.flatMap(previous-> {
					if(meterReading.getReading() < previous.getReading())
						return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "New reading cannot be lower than previous reading"));
					double consumed = meterReading.getReading()- previous.getReading();
					meterReading.setUnitsConsumed(consumed);
					return meterReadingRepo.save(meterReading);
				});
	}
	
	@Override
	public Flux<MeterReading> getReadingsByMeter(String meterId) {
		return meterReadingRepo.findByMeterId(meterId);
	}
}
