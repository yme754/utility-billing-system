package com.utility.meter.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.meter.dto.MeterReadingEvent;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Mono<MeterReading> addReading(MeterReading meterReading) {
        if (meterReading.getDate() == null) {
            meterReading.setDate(LocalDate.now());
        }
        
        return meterReadingRepo.findTopByMeterIdOrderByDateDesc(meterReading.getMeterId())
                .defaultIfEmpty(MeterReading.builder().reading(0.0).build())
                .flatMap(previous -> {
                    if(meterReading.getReading() < previous.getReading()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "New reading (" + meterReading.getReading() + ") cannot be lower than previous (" + previous.getReading() + ")"));
                    }
                    
                    double consumed = meterReading.getReading() - previous.getReading();
                    meterReading.setUnitsConsumed(consumed);
                    
                    return meterReadingRepo.save(meterReading)
                        .flatMap(saved -> {
                            MeterReadingEvent event = new MeterReadingEvent(
                                saved.getId(),
                                saved.getConnectionId(),
                                saved.getMeterId(),
                                saved.getUnitsConsumed(),
                                saved.getDate()
                            );
                            return Mono.fromFuture(kafkaTemplate.send("meter-reading-submitted", event))
                                       .thenReturn(saved);
                        });
                });
    }
    
    @Override
    public Flux<MeterReading> getReadingsForCurrentMonth() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        return meterReadingRepo.findByDateBetween(start, end);
    }
    
    @Override
    public Flux<MeterReading> getReadingsByMeter(String meterId) {
        return meterReadingRepo.findByMeterId(meterId);
    }
}