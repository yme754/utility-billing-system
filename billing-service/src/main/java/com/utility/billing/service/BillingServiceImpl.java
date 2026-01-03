package com.utility.billing.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.utility.billing.dto.AdminStatsDTO;
import com.utility.billing.dto.EmailRequest;
import com.utility.billing.dto.MeterReadingDTO;
import com.utility.billing.dto.MeterReadingEvent;
import com.utility.billing.dto.TariffDTO;
import com.utility.billing.entity.Bill;
import com.utility.billing.entity.Transaction;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService{
	private final WebClient.Builder webClientBuilder;
	private final BillRepository billRepo;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final TransactionRepository transactionRepo;
	
	@Override
    public Mono<Bill> generateAutomatedBill(MeterReadingEvent event) {
        String utilityType = "ELECTRICITY"; 
        Mono<TariffDTO> tariffMono = webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/tariffs?type=" + utilityType)
                .retrieve()
                .bodyToFlux(TariffDTO.class)
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("No Tariff found")));
        return tariffMono.flatMap(tariff -> {
            return calculateAndSaveBill(
                event.getConnectionId(),
                event.getMeterId(),
                event.getUnitsConsumed(),
                tariff
            );
        });
    }

	private Mono<Bill> calculateAndSaveBill(String connId, String meterId, Double units, TariffDTO tariff) {
        double consumedUnits = (units != null) ? units : 0.0;        
        double rate = 0.0;
        if (tariff.getSlabs() != null) {
            rate = tariff.getSlabs().stream()
                .filter(slab -> consumedUnits >= slab.getMinUnits() && consumedUnits <= slab.getMaxUnits())
                .findFirst()
                .map(TariffDTO.Slab::getRatePerUnit)
                .orElse(0.0);
        }
        double energyCharge = consumedUnits * rate;
        double fixedCharge = (tariff.getFixedCharge() != null) ? tariff.getFixedCharge() : 0.0;
        double taxPercent = (tariff.getTaxPercentage() != null) ? tariff.getTaxPercentage() : 0.0;
        
        double tax = (energyCharge + fixedCharge) * (taxPercent / 100);
        double total = energyCharge + fixedCharge + tax;
        Bill bill = Bill.builder()
                .connectionId(connId)
                .meterId(meterId)
                .billingDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .unitsConsumed(consumedUnits)
                .ratePerUnit(rate)
                .fixedCharge(fixedCharge)
                .taxAmount(tax)
                .amount(energyCharge)
                .totalAmount(total)
                .status("UNPAID")
                .build();
        return billRepo.save(bill)
                .flatMap(savedBill -> {
                    log.info("Bill Saved: ID={}", savedBill.getId());
                    EmailRequest email = new EmailRequest(
                        "yxsh2999@gmail.com", 
                        "New Bill Generated", 
                        "Bill of Rs." + savedBill.getTotalAmount() + " generated."
                    );
                    return Mono.fromFuture(kafkaTemplate.send("notification-topic", email))
                            .onErrorResume(e -> {
                                log.warn("Notification Skipped (Kafka Down): {}", e.getMessage());
                                return Mono.empty(); 
                            })
                            .thenReturn(savedBill);
                });
    }

	@Override
    public Mono<Bill> generateBill(String connectionId, String meterId, String utilityName, String token) {
        log.info("Starting Manual Bill Gen: Meter: {}, Utility: {}", meterId, utilityName);
        Mono<MeterReadingDTO> readingMono = webClientBuilder.build().get()
                .uri("http://METER-SERVICE/readings/" + meterId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToFlux(MeterReadingDTO.class)
                .last()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No Readings found for Meter: " + meterId)));
        Mono<TariffDTO> tariffMono = webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/tariffs?type=" + utilityName)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToFlux(TariffDTO.class)
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No Tariff found for Utility: " + utilityName)));
        return Mono.zip(readingMono, tariffMono)
                .flatMap(tuple -> {
                    MeterReadingDTO reading = tuple.getT1();
                    TariffDTO tariff = tuple.getT2();
                    if (tariff == null || tariff.getSlabs() == null || tariff.getSlabs().isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tariff Configuration is Invalid or Empty"));
                    }
                    log.info("Calculation: {} units for Utility: {}", reading.getUnitsConsumed(), utilityName);
                    return calculateAndSaveBill(connectionId, meterId, reading.getUnitsConsumed(), tariff);
                })
                .doOnError(e -> log.error("Billing Failed: {}", e.getMessage()));
    }
    
    @Override
    public Mono<AdminStatsDTO> getAdminStats(String token) {        
        Mono<Long> consumerCountMono = webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/count")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(Long.class)
                .onErrorReturn(0L); 
        Mono<Long> pendingBillsMono = billRepo.countByStatus("UNPAID");
        Mono<Double> revenueMono = billRepo.sumTotalRevenue()
                .map(result -> result.getTotal() != null ? result.getTotal() : 0.0)
                .defaultIfEmpty(0.0);
        return Mono.zip(consumerCountMono, revenueMono, pendingBillsMono)
                .map(tuple -> AdminStatsDTO.builder()
                        .totalConsumers(tuple.getT1())
                        .totalRevenue(tuple.getT2())
                        .pendingBills(tuple.getT3())
                        .build());
    }
    
    @Override
	public Mono<Void> payBill(String billId, String paymentMode) {
		return billRepo.findById(billId)
		    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found")))
		    .flatMap(bill -> {
		        if ("PAID".equals(bill.getStatus())) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill is already paid"));            
		        bill.setStatus("PAID");            
		        Transaction transaction = Transaction.builder()
		                .billId(bill.getId())
		                .amount(bill.getTotalAmount())
		                .paymentMode(paymentMode)
		                .transactionReference("TXN-" + System.currentTimeMillis()) 
		                .timestamp(LocalDateTime.now())
		                .status("SUCCESS")
		                .build();
		        return transactionRepo.save(transaction)
		                .then(billRepo.save(bill));
		    })
		    .then();
	}

    @Override
	public Mono<Bill> getBill(String billId) {
		return billRepo.findById(billId);
	}

	@Override
	public Mono<Void> updateBillStatus(String id, String status) {
		return billRepo.findById(id)
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found")))
				.flatMap(bill -> {
					bill.setStatus(status);
					return billRepo.save(bill);
				}).then();
	}

	@Override
	public Flux<Bill> getPendingBills() {
		return billRepo.findByStatus("UNPAID");
	}

	@Override
	public Flux<Bill> getBillsByConnection(String connectionId) {
		return billRepo.findByConnectionId(connectionId);
	}
}
