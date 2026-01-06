package com.utility.billing.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utility.billing.repository.BillRepository;
import com.utility.payment.entity.Payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentListener {
	private final BillRepository billRepo;
    private final ObjectMapper objectMapper;
    @KafkaListener(topics = "payment-success", groupId = "billing-group")
    public void handlePaymentSuccess(String paymentJson) {
        log.info("Raw Kafka Message Received: {}", paymentJson);
        try {
            Payment payment = objectMapper.readValue(paymentJson, Payment.class);
            log.info("Parsed Payment. Bill ID: {}", payment.getBillId());
            billRepo.findById(payment.getBillId())
                .flatMap(bill -> {
                    bill.setStatus("PAID");
                    log.info("Updating Bill {} status to PAID", bill.getId());
                    return billRepo.save(bill);
                })
                .doOnSuccess(saved -> log.info("DB UPDATE SUCCESS: Bill {} is now PAID", saved.getId()))
                .switchIfEmpty(reactor.core.publisher.Mono.fromRunnable(() -> 
                    log.error("Bill Not Found: {}", payment.getBillId())
                ))
                .subscribe();
        } catch (Exception e) {
            log.error("Error processing payment message: {}", e.getMessage());
        }
    }
}
