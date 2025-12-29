package com.utility.payment.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.utility.payment.entity.Payment;
import com.utility.payment.repository.PaymentRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
	private final PaymentRepository paymentRepo;
	private final WebClient.Builder webClientBuilder;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	
	public Mono<Payment> processPayment(Payment payment) {
		return webClientBuilder.build().get()
				.uri("http://BILLING-SERVICE/bills/" + payment.getBillId())
				.retrieve().bodyToMono(BillDTO.class).flatMap(bill -> {
					payment.setAmount(bill.getTotalAmount());
					payment.setStatus("SUCCESS");
					payment.setTransactionId(UUID.randomUUID().toString());
					payment.setPaymentDate(LocalDateTime.now());
					return paymentRepo.save(payment).flatMap(savedPayment -> 
					webClientBuilder.build().put()
					.uri("http://BILLING-SERVICE/bills/"+ payment.getBillId() + "/status?status?PAID")
					.retrieve().bodyToMono(Void.class).then(Mono.just(savedPayment))
					).doOnSuccess(p -> {
						System.out.println("Payment Event Sent to Kafka for Bill: "+p.getBillId());
					});
				});
	}
	
	@Data
	static class BillDTO {
		private String id;
		private Double totalAmount;
		private String status;
	}
}
