package com.utility.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.utility.billing.dto.MeterReadingEvent;
import com.utility.billing.dto.TariffDTO;
import com.utility.billing.entity.Bill;
import com.utility.billing.entity.Transaction;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.TransactionRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BillingServiceImplTest {

	@Mock
    private BillRepository billRepo;

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private BillingServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new BillingServiceImpl(WebClient.builder(), billRepo, kafkaTemplate, transactionRepo);
    }

    @Test
    void getBill_appliesFineWhenOverdue() {
        Bill bill = Bill.builder()
                .id("b1")
                .status("UNPAID")
                .dueDate(LocalDate.now().minusDays(10))
                .gracePeriod(0)
                .lateFeePerDay(2.0)
                .amount(100.0)
                .fineAmount(0.0)
                .build();

        when(billRepo.findById("b1")).thenReturn(Mono.just(bill));
        when(billRepo.save(any(Bill.class))).thenReturn(Mono.just(bill));

        StepVerifier.create(service.getBill("b1"))
                .expectNextMatches(b -> b.getStatus().equals("OVERDUE") && b.getFineAmount() > 0)
                .verifyComplete();
    }

    @Test
    void cancelBill_unpaid_setsCancelled() {
        Bill bill = Bill.builder().id("b1").status("UNPAID").build();
        when(billRepo.findById("b1")).thenReturn(Mono.just(bill));
        when(billRepo.save(any(Bill.class))).thenReturn(Mono.just(bill));

        StepVerifier.create(service.cancelBill("b1", "reason")).verifyComplete();
        verify(billRepo).save(any(Bill.class));
    }

    @Test
    void cancelBill_paid_throwsError() {
        Bill bill = Bill.builder().id("b1").status("PAID").build();
        when(billRepo.findById("b1")).thenReturn(Mono.just(bill));

        StepVerifier.create(service.cancelBill("b1", "reason"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void payBill_updatesBillAndTransaction() {
        Bill bill = Bill.builder().id("b1").status("UNPAID").totalAmount(200.0).build();
        when(billRepo.findById("b1")).thenReturn(Mono.just(bill));
        when(transactionRepo.save(any(Transaction.class))).thenReturn(Mono.just(Transaction.builder().build()));
        when(billRepo.save(any(Bill.class))).thenReturn(Mono.just(bill));

        StepVerifier.create(service.payBill("b1", "ONLINE")).verifyComplete();
        verify(transactionRepo).save(any(Transaction.class));
        verify(billRepo).save(any(Bill.class));
    }

    @Test
    void getAllBills_appliesFine() {
        Bill bill = Bill.builder().id("b1").status("UNPAID")
                .dueDate(LocalDate.now().minusDays(5))
                .gracePeriod(0).lateFeePerDay(1.0).amount(100.0).build();
        when(billRepo.findAll()).thenReturn(Flux.just(bill));
        when(billRepo.save(any(Bill.class))).thenReturn(Mono.just(bill));

        StepVerifier.create(service.getAllBills())
                .expectNextMatches(b -> b.getStatus().equals("OVERDUE"))
                .verifyComplete();
    }

    @Test
    void calculateEnergyCharge_meteredBaseRate() {
        TariffDTO tariff = TariffDTO.builder()
                .billingType("METERED")
                .baseRate(5.0)
                .build();

        double charge = service.calculateEnergyCharge(10.0, tariff);
        assertEquals(50.0, charge);
    }

    @Test
    void sendPaymentReminder_updatesLastReminder() {
        Bill bill = Bill.builder()
                .id("b1")
                .status("UNPAID")
                .dueDate(LocalDate.now().minusDays(1))
                .totalAmount(200.0)
                .build();

        when(billRepo.findById("b1")).thenReturn(Mono.just(bill));
        when(billRepo.save(any(Bill.class))).thenReturn(Mono.just(bill));

        StepVerifier.create(service.sendPaymentReminder("b1"))
                .verifyComplete();

        verify(billRepo).save(any(Bill.class));
        assertNotNull(bill.getLastReminderSent());
        assertEquals("UNPAID", bill.getStatus());
    }

    @Test
    void calculateEnergyCharge_subscriptionIsZero() {
        TariffDTO tariff = TariffDTO.builder().billingType("SUBSCRIPTION").baseRate(100.0).build();
        assertEquals(0.0, service.calculateEnergyCharge(50.0, tariff));
    }

    @Test
    void calculateEnergyCharge_meteredWithSlab() {
        TariffDTO.Slab slab = new TariffDTO.Slab();
        slab.setMinUnits(0);
        slab.setMaxUnits(100);
        slab.setRate(2.0);
        TariffDTO tariff = TariffDTO.builder().billingType("METERED").slabs(List.of(slab)).build();
        assertEquals(20.0, service.calculateEnergyCharge(10.0, tariff));
    }

    @Test
    void updateBillStatus_setsStatus() {
        Bill bill = Bill.builder().id("b1").status("UNPAID").build();
        when(billRepo.findById("b1")).thenReturn(Mono.just(bill));
        when(billRepo.save(any(Bill.class))).thenReturn(Mono.just(bill));

        StepVerifier.create(service.updateBillStatus("b1", "PAID")).verifyComplete();
        assertEquals("PAID", bill.getStatus());
    }

    @Test
    void calculateAndSave_createsBill() {
        TariffDTO tariff = TariffDTO.builder()
                .billingType("METERED")
                .baseRate(5.0)
                .taxPercentage(10.0)
                .utilityType("ELECTRICITY")
                .planName("Residential")
                .build();

        when(billRepo.save(any(Bill.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.calculateAndSave("c1", "m1", 0.0, 10.0, tariff))
                .expectNextMatches(b -> b.getUnitsConsumed() == 10.0 && b.getTotalAmount() > 0)
                .verifyComplete();
    }

    @Test
    void getAdminStats_returnsAggregatedValues() {
        when(billRepo.countByStatus("UNPAID")).thenReturn(Mono.just(5L));

        BillRepository.RevenueResult revenueResult = new BillRepository.RevenueResult() { 
        	@Override public Double getTotal() { 
        		return 1000.0; 
        		} 
        	};
        when(billRepo.sumTotalRevenue()).thenReturn(Mono.just(revenueResult));

        StepVerifier.create(service.getAdminStats("token"))
                .expectNextMatches(stats -> stats.getPendingBills() == 5L && stats.getTotalRevenue() == 1000.0)
                .verifyComplete();
    }

    @Test
    void calculateAndSave_createsBillWithTaxAndFixedCharge() {
        TariffDTO tariff = TariffDTO.builder()
                .billingType("METERED")
                .baseRate(5.0)
                .fixedCharge(20.0)
                .taxPercentage(10.0)
                .utilityType("ELECTRICITY")
                .planName("Residential")
                .build();

        when(billRepo.save(any(Bill.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.calculateAndSave("c1", "m1", 0.0, 10.0, tariff))
                .expectNextMatches(b -> b.getUnitsConsumed() == 10.0 && b.getTotalAmount() > 0)
                .verifyComplete();
    }

    @Test
    void getPendingBills_returnsFlux() {
        Bill bill = Bill.builder().id("b1").status("UNPAID").build();
        when(billRepo.findByStatus("UNPAID")).thenReturn(Flux.just(bill));

        StepVerifier.create(service.getPendingBills())
                .expectNext(bill)
                .verifyComplete();
    }

    @Test
    void getBillsByConnection_returnsFlux() {
        Bill bill = Bill.builder().id("b1").connectionId("c1").build();
        when(billRepo.findByConnectionId("c1")).thenReturn(Flux.just(bill));

        StepVerifier.create(service.getBillsByConnection("c1"))
                .expectNext(bill)
                .verifyComplete();
    }

    @Test
    void calculateEnergyCharge_meteredWithRatePerUnit() {
        TariffDTO.Slab slab = new TariffDTO.Slab();
        slab.setMinUnits(0);
        slab.setMaxUnits(100);
        slab.setRatePerUnit(3.0);

        TariffDTO tariff = TariffDTO.builder()
                .billingType("METERED")
                .slabs(List.of(slab))
                .build();

        assertEquals(30.0, service.calculateEnergyCharge(10.0, tariff));
    }

    @Test
    void calculateEnergyCharge_noMatchingSlab_returnsZero() {
        TariffDTO.Slab slab = new TariffDTO.Slab();
        slab.setMinUnits(50);
        slab.setMaxUnits(100);
        slab.setRate(2.0);

        TariffDTO tariff = TariffDTO.builder()
                .billingType("METERED")
                .slabs(List.of(slab))
                .build();

        assertEquals(0.0, service.calculateEnergyCharge(10.0, tariff));
    }

    @Test
    void calculateAndSave_subscriptionBillingTypeUsesBaseRate() {
        TariffDTO tariff = TariffDTO.builder()
                .billingType("SUBSCRIPTION")
                .baseRate(100.0)
                .taxPercentage(5.0)
                .utilityType("INTERNET")
                .planName("Broadband")
                .build();

        when(billRepo.save(any(Bill.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.calculateAndSave("c1", "m1", 0.0, 0.0, tariff))
                .expectNextMatches(b -> b.getFixedCharge() == 100.0 && b.getTotalAmount() > 0)
                .verifyComplete();
    }

    @Test
    void calculateAndSave_triggersInvoiceNotification() {
        TariffDTO tariff = TariffDTO.builder()
                .billingType("METERED")
                .baseRate(5.0)
                .utilityType("ELECTRICITY")
                .planName("Residential")
                .build();

        when(billRepo.save(any(Bill.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        service.calculateAndSave("c1", "m1", 0.0, 10.0, tariff).block();

        verify(kafkaTemplate).send(eq("notification-topic"), any());
    }

    @Test
    void updateBillStatus_billNotFoundCompletesEmpty() {
        when(billRepo.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(service.updateBillStatus("missing", "PAID"))
                .verifyComplete();
    }

    @Test
    void generateBill_invokesFlow_errorPath() {
        StepVerifier.create(service.generateBill("c1", "m1", "Electricity", "token"))
                .expectError()
                .verify();
    }

    @Test
    void generateAutomatedBill_invokesFlow_errorPath() {
        MeterReadingEvent event = MeterReadingEvent.builder()
                .connectionId("c1")
                .meterId("m1")
                .unitsConsumed(50.0)
                .build();

        StepVerifier.create(service.generateAutomatedBill(event))
                .expectError()
                .verify();
    }

}