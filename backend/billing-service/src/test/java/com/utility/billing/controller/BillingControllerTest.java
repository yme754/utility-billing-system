package com.utility.billing.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.utility.billing.dto.BillRequestDTO;
import com.utility.billing.entity.Bill;
import com.utility.billing.service.BillingService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class BillingControllerTest {
	

    private WebTestClient webTestClient;
    private BillingService billingService;

    @BeforeEach
    void setup() {
        billingService = Mockito.mock(BillingService.class);
        BillingController controller = new BillingController(billingService);
        this.webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void generateBill_success() {
        BillRequestDTO req = BillRequestDTO.builder()
                .connectionId("c1").meterId("m1").utilityName("Electricity").build();
        Bill bill = Bill.builder().id("b1").connectionId("c1").amount(100.0).build();

        Mockito.when(billingService.generateBill("c1", "m1", "Electricity", "Bearer token"))
                .thenReturn(Mono.just(bill));

        webTestClient.post().uri("/bills/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Bill.class)
                .isEqualTo(bill);
    }

    @Test
    void getBill_success() {
        Bill bill = Bill.builder().id("b1").connectionId("c1").amount(100.0).build();
        Mockito.when(billingService.getBill("b1")).thenReturn(Mono.just(bill));

        webTestClient.get().uri("/bills/b1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Bill.class)
                .isEqualTo(bill);
    }

    @Test
    void updateBillStatus_success() {
        Mockito.when(billingService.updateBillStatus("b1", "PAID")).thenReturn(Mono.empty());

        webTestClient.put().uri("/bills/b1/status?status=PAID")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getPendingBills_success() {
        Bill bill = Bill.builder().id("b1").connectionId("c1").amount(100.0).build();
        Mockito.when(billingService.getPendingBills()).thenReturn(Flux.just(bill));

        webTestClient.get().uri("/bills/pending")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Bill.class)
                .hasSize(1);
    }

    @Test
    void getMyBills_success() {
        Bill bill = Bill.builder().id("b1").connectionId("c1").amount(100.0).build();
        Mockito.when(billingService.getBillsByConnection("c1")).thenReturn(Flux.just(bill));

        webTestClient.get().uri("/bills/my-bills/c1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Bill.class)
                .hasSize(1);
    }

    @Test
    void payBill_success() {
        Mockito.when(billingService.payBill("b1", "ONLINE")).thenReturn(Mono.empty());

        webTestClient.put().uri("/bills/b1/pay?mode=ONLINE")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getAllBills_success() {
        Bill bill = Bill.builder().id("b1").connectionId("c1").amount(100.0).build();
        Mockito.when(billingService.getAllBills()).thenReturn(Flux.just(bill));

        webTestClient.get().uri("/bills/all")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Bill.class)
                .hasSize(1);
    }

    @Test
    void cancelBill_success() {
        Mockito.when(billingService.cancelBill("b1", "Duplicate")).thenReturn(Mono.empty());

        webTestClient.put().uri("/bills/b1/cancel?reason=Duplicate")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void sendReminder_success() {
        Mockito.when(billingService.sendPaymentReminder("b1")).thenReturn(Mono.empty());

        webTestClient.post().uri("/bills/b1/reminder")
                .exchange()
                .expectStatus().isOk();
    }
}