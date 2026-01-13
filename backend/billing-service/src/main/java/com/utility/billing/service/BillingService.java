package com.utility.billing.service;

import java.time.LocalDateTime;

import com.utility.billing.dto.AdminStatsDTO;
import com.utility.billing.dto.MeterReadingEvent;
import com.utility.billing.entity.Bill;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BillingService {
	Mono<Bill> generateBill(String connectionId, String meterId, String utilityName, String token);
	Mono<Bill> getBill(String billId);
	Mono<Void> updateBillStatus(String id, String status);
	Flux<Bill> getPendingBills();
    Flux<Bill> getBillsByConnection(String connectionId);
    Mono<AdminStatsDTO> getAdminStats(String token);
    Mono<Void> payBill(String billId, String paymentMode);
    Mono<Bill> generateAutomatedBill(MeterReadingEvent event);
    Flux<Bill> getAllBills();
	Mono<Void> cancelBill(String id, String reason);
	Mono<Void> sendPaymentReminder(String billId);
	Mono<Void> scheduleUserReminder(String billId, LocalDateTime scheduledTime);
}
