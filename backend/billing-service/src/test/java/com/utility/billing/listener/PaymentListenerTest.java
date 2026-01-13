package com.utility.billing.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utility.billing.entity.Bill;
import com.utility.billing.repository.BillRepository;
import com.utility.payment.entity.Payment;

import reactor.core.publisher.Mono;

class PaymentListenerTest {

    private BillRepository billRepo;
    private ObjectMapper objectMapper;
    private PaymentListener listener;

    @BeforeEach
    void setup() {
        billRepo = Mockito.mock(BillRepository.class);
        objectMapper = new ObjectMapper();
        listener = new PaymentListener(billRepo, objectMapper, null);
    }

    @Test
    void handlePaymentSuccess_updatesBillToPaid() throws Exception {
        Payment payment = new Payment();
        payment.setBillId("b1");
        String json = objectMapper.writeValueAsString(payment);

        Bill bill = Bill.builder().id("b1").status("UNPAID").build();
        when(billRepo.findById("b1")).thenReturn(Mono.just(bill));
        when(billRepo.save(any(Bill.class))).thenReturn(Mono.just(bill));

        listener.handlePaymentSuccess(json);

        verify(billRepo).save(any(Bill.class));
        assertEquals("PAID", bill.getStatus());
    }

    @Test
    void handlePaymentSuccess_billNotFound_logsError() throws Exception {
        Payment payment = new Payment();
        payment.setBillId("missing");
        String json = objectMapper.writeValueAsString(payment);

        when(billRepo.findById("missing")).thenReturn(Mono.empty());

        listener.handlePaymentSuccess(json);

        verify(billRepo, never()).save(any(Bill.class));
    }

    @Test
    void handlePaymentSuccess_invalidJson_logsError() {
        String badJson = "{not-valid}";

        listener.handlePaymentSuccess(badJson);

        verifyNoInteractions(billRepo);
    }
}
