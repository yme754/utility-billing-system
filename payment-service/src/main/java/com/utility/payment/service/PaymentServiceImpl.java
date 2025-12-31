package com.utility.payment.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
@EnableReactiveMethodSecurity
public class PaymentServiceImpl implements PaymentService{
	private final PaymentRepository paymentRepo;
	private final WebClient.Builder webClientBuilder;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	
	@Override
	public Mono<Payment> processPayment(Payment payment, String token) {
		log.info("Processing payment for billId: {}", payment.getBillId());
        return webClientBuilder.build().get()
                .uri("http://BILLING-SERVICE/bills/" + payment.getBillId())
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(BillDTO.class)
                .flatMap(bill -> {
                    payment.setAmount(bill.getTotalAmount());
                    payment.setStatus("SUCCESS");
                    payment.setTransactionId(UUID.randomUUID().toString());
                    payment.setPaymentDate(LocalDateTime.now());                    
                    return paymentRepo.save(payment).flatMap(savedPayment -> 
                        webClientBuilder.build().put()
                                .uri("http://BILLING-SERVICE/bills/" + payment.getBillId() + "/status?status=PAID")
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .then(Mono.just(savedPayment))
                    ).doOnSuccess(p -> {
                        log.info("Sending Payment Notification for Bill: {}", p.getBillId());
                        EmailRequest email = new EmailRequest(
                                "yxsh2999@gmail.com",
                                "Payment Successful",
                                "Your payment of ₹" + p.getAmount() + " for Bill " + p.getBillId() + " was successful. Trans ID: " + p.getTransactionId()
                        );
                        kafkaTemplate.send("notification-topic", email);
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
