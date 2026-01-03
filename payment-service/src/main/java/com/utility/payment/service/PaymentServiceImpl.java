package com.utility.payment.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.utility.payment.dto.PaymentRequest;
import com.utility.payment.entity.Payment;
import com.utility.payment.repository.PaymentRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRepository paymentRepo;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Mono<Payment> processPayment(PaymentRequest request, String token) {
        log.info("Processing Mock Payment for Bill: {}", request.getBillId());
        return webClientBuilder.build().get()
                .uri("http://BILLING-SERVICE/bills/" + request.getBillId())
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(BillDTO.class)
                .flatMap(bill -> {
                    if ("PAID".equals(bill.getStatus())) {
                        return Mono.error(new RuntimeException("This bill is already PAID!"));
                    }
                    Payment payment = Payment.builder()
                            .billId(request.getBillId())
                            .amount(bill.getTotalAmount())
                            .paymentMode(request.getPaymentMode())
                            .status("SUCCESS")
                            .transactionId("TXN-" + UUID.randomUUID().toString())
                            .paymentDate(LocalDateTime.now())
                            .build();                    
                    return paymentRepo.save(payment).flatMap(savedPayment -> {
                        kafkaTemplate.send("payment-success", savedPayment);
                        log.info("Event 'payment-success' sent to Kafka");
                        EmailRequest email = new EmailRequest(
                            "yxsh2999@gmail.com",
                            "Payment Successful",
                            "Payment of Rs. " + savedPayment.getAmount() + " successful. Ref: " + savedPayment.getTransactionId()
                        );
                        kafkaTemplate.send("notification-topic", email);

                        return Mono.just(savedPayment);
                    });
                });
    }

    @Override
    public Flux<Payment> getSuccessfulPayments() {
        return paymentRepo.findByStatus("SUCCESS");
    }
    
    @Data
    static class BillDTO {
        private String id;
        private Double totalAmount;
        private String status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class EmailRequest {
        private String to;
        private String subject;
        private String body;
    }
}