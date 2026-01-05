package com.utility.billing.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    public Mono<Bill> generateBill(String connectionId, String meterId, String utilityName, String token) {
        log.info("Generating bill for Meter: {}", meterId);
        return webClientBuilder.build().get()
                .uri("http://METER-SERVICE/readings/" + meterId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToFlux(MeterReadingDTO.class)
                .last()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No Meter Reading found for " + meterId)))
                .flatMap(reading -> 
                    fetchTariffAndCalculate(connectionId, meterId, utilityName, reading.getUnitsConsumed(), token)
                );
    }
    
    private Mono<Bill> fetchTariffAndCalculate(String connId, String meterId, String utility, Double currentReading, String token) {
        return webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/tariffs?type=" + utility)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToFlux(TariffDTO.class)
                .next() 
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No Tariff Plan found for " + utility)))
                .flatMap(tariff -> 
                    billRepo.findFirstByConnectionIdOrderByBillingDateDesc(connId)
                            .map(Bill::getCurrentReading)
                            .defaultIfEmpty(0.0)
                            .flatMap(prevReading -> calculateAndSave(connId, meterId, prevReading, currentReading, tariff))
                );
    }

    private Mono<Bill> calculateAndSave(String connId, String meterId, Double prev, Double curr, TariffDTO tariff) {
        double consumed = Math.max(0, curr - prev);
        double energyCharge = calculateEnergyCharge(consumed, tariff);
        double fixed = tariff.getFixedCharge() != null ? tariff.getFixedCharge() : 0.0;
        double taxPercent = tariff.getTaxPercentage() != null ? tariff.getTaxPercentage() : 0.0;
        double taxAmount = (energyCharge + fixed) * (taxPercent / 100);
        double baseAmount = energyCharge + fixed + taxAmount;
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(30);         
        double lateFee = tariff.getLateFeePerDay() != null ? tariff.getLateFeePerDay() : 0.0;
        int grace = tariff.getGracePeriodDays() != null ? tariff.getGracePeriodDays() : 0;
        Bill bill = Bill.builder()
                .connectionId(connId).meterId(meterId)
                .billingDate(today).dueDate(dueDate)
                .previousReading(prev).currentReading(curr).unitsConsumed(consumed)
                .ratePerUnit(consumed > 0 ? energyCharge / consumed : 0)
                .fixedCharge(fixed).taxAmount(taxAmount)
                .amount(baseAmount)      
                .fineAmount(0.0)         
                .totalAmount(baseAmount)                 
                .lateFeePerDay(lateFee)
                .gracePeriod(grace)
                .status("UNPAID")
                .build();
        return billRepo.save(bill).flatMap(saved -> {
            sendInvoiceNotification(saved, consumed, baseAmount);
            return Mono.just(saved);
        });
    }

    private double calculateEnergyCharge(double consumed, TariffDTO tariff) {
        String type = tariff.getBillingType() != null ? tariff.getBillingType() : "METERED";
        
        if ("METERED".equalsIgnoreCase(type)) {
            if (tariff.getSlabs() != null && !tariff.getSlabs().isEmpty()) {
                double rate = tariff.getSlabs().stream()
                    .filter(s -> consumed >= s.getMinUnits() && consumed <= s.getMaxUnits())
                    .findFirst()
                    .map(s -> s.getRate() != null ? s.getRate() : (s.getRatePerUnit() != null ? s.getRatePerUnit() : 0.0))
                    .orElse(0.0);
                return consumed * rate;
            } else {
                return consumed * (tariff.getBaseRate() != null ? tariff.getBaseRate() : 0.0);
            }
        } else {
            double baseRate = tariff.getBaseRate() != null ? tariff.getBaseRate() : 0.0;
            return (consumed > 0 ? consumed : 1) * baseRate; 
        }
    }

    public Mono<Bill> checkAndApplyFine(Bill bill) {
        if ("PAID".equals(bill.getStatus()) || "CANCELLED".equals(bill.getStatus())) {
            return Mono.just(bill);
        }
        long daysLate = ChronoUnit.DAYS.between(bill.getDueDate(), LocalDate.now());        
        int grace = bill.getGracePeriod() != null ? bill.getGracePeriod() : 0;
        double finePerDay = bill.getLateFeePerDay() != null ? bill.getLateFeePerDay() : 0.0;
        double newFine = 0.0;
        if (daysLate > grace) {
            newFine = (daysLate - grace) * finePerDay;
        }
        double currentFine = bill.getFineAmount() != null ? bill.getFineAmount() : 0.0;
        if (Double.compare(newFine, currentFine) != 0) {
            bill.setFineAmount(newFine);
            bill.setTotalAmount(bill.getAmount() + newFine);
            bill.setStatus("OVERDUE");
            return billRepo.save(bill);
        }
        return Mono.just(bill);
    }
    
    @Override 
    public Mono<Bill> getBill(String id) { 
        return billRepo.findById(id)
                .flatMap(this::checkAndApplyFine);
    }

    @Override
    public Mono<Void> cancelBill(String id, String reason) {
        return billRepo.findById(id)
            .flatMap(bill -> {
                if ("PAID".equals(bill.getStatus())) {
                    return Mono.error(new RuntimeException("Cannot cancel a PAID bill"));
                }
                bill.setStatus("CANCELLED");
                return billRepo.save(bill);
            }).then();
    }

    private void sendInvoiceNotification(Bill saved, double consumed, double total) {
        try {
            EmailRequest email = EmailRequest.builder()
                    .to("yxsh2999@gmail.com") 
                    .subject("New Utility Invoice - " + LocalDate.now().getMonth())
                    .body("Consumption: " + consumed + " units. Total: ₹" + total)
                    .isInvoice(true)
                    .billId(saved.getId())
                    .amount(total)
                    .build();
            kafkaTemplate.send("notification-topic", email);
        } catch (Exception e) {
            log.error("Failed to queue email notification", e);
        }
    }

    @Override
    public Flux<Bill> getAllBills() {
        return billRepo.findAll()
                .flatMap(this::checkAndApplyFine);
    }

    @Override
    public Mono<Bill> generateAutomatedBill(MeterReadingEvent event) {
        return webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/tariffs?type=ELECTRICITY")
                .retrieve()
                .bodyToFlux(TariffDTO.class).next()
                .flatMap(t -> {
                    return billRepo.findFirstByConnectionIdOrderByBillingDateDesc(event.getConnectionId())
                            .map(Bill::getCurrentReading).defaultIfEmpty(0.0)
                            .flatMap(prev -> calculateAndSave(event.getConnectionId(), event.getMeterId(), prev, event.getUnitsConsumed(), t));
                });
    }

    @Override
    public Mono<AdminStatsDTO> getAdminStats(String token) {
        Mono<Long> consumerCount = webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/count")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve().bodyToMono(Long.class).onErrorReturn(0L);
        Mono<Long> pending = billRepo.countByStatus("UNPAID");
        Mono<Double> revenue = billRepo.sumTotalRevenue()
                .map(res -> res.getTotal() != null ? res.getTotal() : 0.0).defaultIfEmpty(0.0);
        return Mono.zip(consumerCount, revenue, pending)
                .map(t -> new AdminStatsDTO(t.getT1(), t.getT2(), t.getT3()));
    }

    @Override
    public Mono<Void> payBill(String billId, String paymentMode) {
        return billRepo.findById(billId)
                .flatMap(bill -> {
                    bill.setStatus("PAID");
                    Transaction tx = Transaction.builder()
                            .billId(bill.getId()).amount(bill.getTotalAmount())
                            .paymentMode(paymentMode).status("SUCCESS")
                            .timestamp(LocalDateTime.now())
                            .transactionReference("TXN-" + System.currentTimeMillis()).build();
                    return transactionRepo.save(tx).then(billRepo.save(bill));
                }).then();
    }

    @Override public Flux<Bill> getPendingBills() { return billRepo.findByStatus("UNPAID"); }
    @Override public Flux<Bill> getBillsByConnection(String cid) { return billRepo.findByConnectionId(cid); }
    @Override public Mono<Void> updateBillStatus(String id, String s) { 
        return billRepo.findById(id).flatMap(b -> { b.setStatus(s); return billRepo.save(b); }).then(); 
    }
}