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
import com.utility.billing.dto.ConnectionDTO;
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
    
    private static final String STATUS_UNPAID = "UNPAID";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String NOTIFICATION_TOPIC = "notification-topic";
    private static final String ADMIN_EMAIL = "yxsh2999@gmail.com";
    private static final String TYPE_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String TYPE_METERED = "METERED";

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
                    fetchSpecificTariffAndCalculate(connectionId, meterId, utilityName, reading.getUnitsConsumed(), token)
                );
    }
    
    private Mono<Bill> fetchSpecificTariffAndCalculate(String connId, String meterId, String utility, Double currentReading, String token) {        
        return webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/connections/" + connId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(ConnectionDTO.class)
                .flatMap(connection -> {
                    String subscribedPlan = connection.getTariffCategory(); 
                    
                    return webClientBuilder.build().get()
                            .uri("http://CONSUMER-SERVICE/consumers/tariffs?type=" + utility)
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .retrieve()
                            .bodyToFlux(TariffDTO.class)
                            .filter(t -> (t.getPlanName() != null && t.getPlanName().equalsIgnoreCase(subscribedPlan)) || 
                                         (t.getCategory() != null && t.getCategory().equalsIgnoreCase(subscribedPlan)))
                            .next()
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff Plan not found: " + subscribedPlan)))
                            .flatMap(tariff -> 
                                billRepo.findFirstByConnectionIdOrderByBillingDateDesc(connId)
                                        .map(Bill::getCurrentReading)
                                        .defaultIfEmpty(0.0)
                                        .flatMap(prevReading -> calculateAndSave(connId, meterId, prevReading, currentReading, tariff))
                            );
                });
    }

    Mono<Bill> calculateAndSave(String connId, String meterId, Double prev, Double curr, TariffDTO tariff) {
        double consumed = Math.max(0, curr - prev);        
        double energyCharge = calculateEnergyCharge(consumed, tariff);
        double fixedCost = TYPE_SUBSCRIPTION.equalsIgnoreCase(tariff.getBillingType()) 
                ? (tariff.getBaseRate() != null ? tariff.getBaseRate() : 0.0)
                : (tariff.getFixedCharge() != null ? tariff.getFixedCharge() : 0.0);

        double taxPercent = tariff.getTaxPercentage() != null ? tariff.getTaxPercentage() : 0.0;
        double taxAmount = (energyCharge + fixedCost) * (taxPercent / 100);
        
        double baseAmount = energyCharge + fixedCost + taxAmount;
        
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(30);          
        
        double lateFee = tariff.getLateFeePerDay() != null ? tariff.getLateFeePerDay() : 0.0;
        int grace = tariff.getGracePeriodDays() != null ? tariff.getGracePeriodDays() : 0;
        
        Bill bill = Bill.builder()
                .connectionId(connId).meterId(meterId)
                .billingDate(today).dueDate(dueDate)
                .previousReading(prev).currentReading(curr).unitsConsumed(consumed)
                .ratePerUnit(consumed > 0 ? energyCharge / consumed : 0)
                .fixedCharge(fixedCost).taxAmount(taxAmount)
                .amount(baseAmount)      
                .fineAmount(0.0)          
                .totalAmount(baseAmount)                  
                .lateFeePerDay(lateFee)
                .gracePeriod(grace)
                .status(STATUS_UNPAID)
                .utilityType(tariff.getUtilityType())
                .tariffPlanName(tariff.getPlanName())
                .build();
                
        return billRepo.save(bill).flatMap(saved -> {
            sendInvoiceNotification(saved, consumed, baseAmount);
            return Mono.just(saved);
        });
    }

     double calculateEnergyCharge(double consumed, TariffDTO tariff) {
        String type = tariff.getBillingType() != null ? tariff.getBillingType() : TYPE_METERED;        
        if (TYPE_SUBSCRIPTION.equalsIgnoreCase(type)) {
            return 0.0; 
        }        
        if (TYPE_METERED.equalsIgnoreCase(type)) {
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
        } 
        
        return 0.0;
    }

    public Mono<Bill> checkAndApplyFine(Bill bill) {
        if (STATUS_PAID.equals(bill.getStatus()) || STATUS_CANCELLED.equals(bill.getStatus())) {
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
        return billRepo.findById(id).flatMap(this::checkAndApplyFine);
    }

    @Override
    public Mono<Void> cancelBill(String id, String reason) {
        return billRepo.findById(id)
            .flatMap(bill -> {
                if (STATUS_PAID.equals(bill.getStatus())) {
                    return Mono.error(new RuntimeException("Cannot cancel a PAID bill"));
                }
                bill.setStatus(STATUS_CANCELLED);
                return billRepo.save(bill);
            }).then();
    }

    private void sendInvoiceNotification(Bill saved, double consumed, double total) {
        try {
            EmailRequest email = EmailRequest.builder()
                    .to(ADMIN_EMAIL) 
                    .subject("New Utility Invoice")
                    .body("Consumption: " + consumed + " units. Total: ₹" + total)
                    .isInvoice(true)
                    .billId(saved.getId())
                    .amount(total)
                    .build();
            kafkaTemplate.send(NOTIFICATION_TOPIC, email);
        } catch (Exception e) {
            log.error("Failed to queue email notification", e);
        }
    }

    @Override
    public Flux<Bill> getAllBills() {
        return billRepo.findAll().flatMap(this::checkAndApplyFine);
    }

    @Override
    public Mono<Bill> generateAutomatedBill(MeterReadingEvent event) {
        return webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/connections/" + event.getConnectionId())
                .retrieve()
                .bodyToMono(ConnectionDTO.class)
                .flatMap(connection -> {
                    String subscribedPlan = connection.getTariffCategory();
                    return webClientBuilder.build().get()
                        .uri("http://CONSUMER-SERVICE/consumers/tariffs?type=ELECTRICITY")
                        .retrieve()
                        .bodyToFlux(TariffDTO.class)
                        .filter(t -> (t.getPlanName() != null && t.getPlanName().equalsIgnoreCase(subscribedPlan)) || 
                                     (t.getCategory() != null && t.getCategory().equalsIgnoreCase(subscribedPlan)))
                        .next()
                        .flatMap(t -> {
                            return billRepo.findFirstByConnectionIdOrderByBillingDateDesc(event.getConnectionId())
                                    .map(Bill::getCurrentReading).defaultIfEmpty(0.0)
                                    .flatMap(prev -> calculateAndSave(event.getConnectionId(), event.getMeterId(), prev, event.getUnitsConsumed(), t));
                        });
                });
    }

    @Override
    public Mono<AdminStatsDTO> getAdminStats(String token) {
        Mono<Long> consumerCount = webClientBuilder.build().get()
                .uri("http://CONSUMER-SERVICE/consumers/count")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve().bodyToMono(Long.class).onErrorReturn(0L);
        Mono<Long> pending = billRepo.countByStatus(STATUS_UNPAID);
        Mono<Double> revenue = billRepo.sumTotalRevenue()
                .map(res -> res.getTotal() != null ? res.getTotal() : 0.0).defaultIfEmpty(0.0);
        return Mono.zip(consumerCount, revenue, pending)
                .map(t -> new AdminStatsDTO(t.getT1(), t.getT2(), t.getT3()));
    }

    @Override
    public Mono<Void> payBill(String billId, String paymentMode) {
        return billRepo.findById(billId)
                .flatMap(bill -> {
                    bill.setStatus(STATUS_PAID);
                    bill.setPaymentMode(paymentMode);
                    bill.setPaymentDate(LocalDateTime.now());
                    
                    Transaction tx = Transaction.builder()
                            .billId(bill.getId()).amount(bill.getTotalAmount())
                            .paymentMode(paymentMode).status("SUCCESS")
                            .timestamp(LocalDateTime.now())
                            .transactionReference("TXN-" + System.currentTimeMillis()).build();
                    return transactionRepo.save(tx).then(billRepo.save(bill));
                }).then();
    }
    
    @Override
    public Mono<Void> sendPaymentReminder(String billId) {
        return billRepo.findById(billId)
                .flatMap(bill -> {
                    String userEmail = ADMIN_EMAIL; 
                    LocalDate today = LocalDate.now();
                    long daysDiff = ChronoUnit.DAYS.between(today, bill.getDueDate());
                    
                    String subject = daysDiff >= 0 ? "Reminder: Bill Due Soon" : "URGENT: Bill Overdue";
                    String body = "Dear Customer, please pay your bill of ₹" + bill.getTotalAmount();

                    try {
                        EmailRequest email = EmailRequest.builder()
                                .to(userEmail).subject(subject).body(body)
                                .isInvoice(false).billId(bill.getId()).amount(bill.getTotalAmount()).build();
                        kafkaTemplate.send(NOTIFICATION_TOPIC, email);
                    } catch (Exception e) {
                        log.error("Failed to queue email notification", e);
                    }
                    bill.setLastReminderSent(LocalDateTime.now());
                    return billRepo.save(bill);
                }).then();
    }

    @Override public Flux<Bill> getPendingBills() { return billRepo.findByStatus(STATUS_UNPAID); }
    @Override public Flux<Bill> getBillsByConnection(String cid) { return billRepo.findByConnectionId(cid); }
    @Override public Mono<Void> updateBillStatus(String id, String s) { 
        return billRepo.findById(id).flatMap(b -> { b.setStatus(s); return billRepo.save(b); }).then(); 
    }
}