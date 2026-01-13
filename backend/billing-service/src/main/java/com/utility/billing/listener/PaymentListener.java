package com.utility.billing.listener;

import java.time.LocalDateTime;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utility.billing.entity.Transaction;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.TransactionRepository;
import com.utility.payment.entity.Payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentListener {
	private final BillRepository billRepo;
    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepo;
    @KafkaListener(topics = "payment-success", groupId = "billing-group")
    public void handlePaymentSuccess(String paymentJson) {
        log.info("Billing Service Received Payment Event: {}", paymentJson);
        try {
            Payment payment = objectMapper.readValue(paymentJson, Payment.class);
            Transaction transaction = Transaction.builder()
                    .billId(payment.getBillId())
                    .amount(payment.getAmount())
                    .paymentMode(payment.getPaymentMode())
                    .transactionReference(payment.getTransactionId())
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            billRepo.findById(payment.getBillId())
                    .flatMap(bill -> {
                        bill.setStatus("PAID");
                        bill.setPaymentDate(LocalDateTime.now());
                        bill.setPaymentMode(payment.getPaymentMode());
                        log.info("Updating Bill {} status to PAID", bill.getId());                        
                        return billRepo.save(bill)
                                .then(transactionRepo.save(transaction));
                    })
                    .doOnSuccess(txn -> log.info("Sync Complete: Bill PAID & Transaction Saved: {}", txn.getId()))
                    .doOnError(e -> log.error("Failed to sync payment data: {}", e.getMessage()))
                    .block(); 

        } catch (Exception e) {
            log.error("CRITICAL: Error processing payment event: {}", e.getMessage());
        }
    }
}
