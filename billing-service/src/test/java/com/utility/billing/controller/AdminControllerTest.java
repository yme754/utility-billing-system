package com.utility.billing.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.utility.billing.dto.AdminStatsDTO;
import com.utility.billing.service.BillingService;

import reactor.core.publisher.Mono;

 class AdminControllerTest {

    private WebTestClient webTestClient;
    private BillingService billingService;

    @BeforeEach
    void setup() {
        billingService = Mockito.mock(BillingService.class);
        AdminController controller = new AdminController(billingService);
        this.webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void getAdminStats_success() {
        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalConsumers(100L)
                .totalRevenue(5000.0)
                .pendingBills(5L)
                .build();

        Mockito.when(billingService.getAdminStats("Bearer token")).thenReturn(Mono.just(stats));

        webTestClient.get()
                .uri("/bills/admin/stats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AdminStatsDTO.class)
                .isEqualTo(stats);
    }

}
