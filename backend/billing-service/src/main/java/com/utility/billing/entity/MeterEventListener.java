package com.utility.billing.entity;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.utility.billing.dto.MeterReadingEvent;
import com.utility.billing.service.BillingService;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;


@Component
@Slf4j
@RequiredArgsConstructor
public class MeterEventListener {
	private final BillingService billingService;
	
	@KafkaListener(topics = "meter-reading-submitted", groupId = "billing-group")
    public void handleMeterReading(MeterReadingEvent event) {
        log.info("Received Meter Reading Event for Meter: {}", event.getMeterId());        
        billingService.generateAutomatedBill(event)
            .subscribe(
                bill -> log.info("Auto-Bill Generated: ID={}, Amount={}", bill.getId(), bill.getTotalAmount()),
                error -> log.error("Failed to generate bill for meter {}", event.getMeterId(), error)
            );
    }
}
