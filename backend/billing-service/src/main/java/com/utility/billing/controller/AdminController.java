package com.utility.billing.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utility.billing.dto.AdminStatsDTO;
import com.utility.billing.service.BillingService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bills/admin")
@RequiredArgsConstructor
public class AdminController {
	private final BillingService billingService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<AdminStatsDTO>> getAdminStats(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {        
        return billingService.getAdminStats(token).map(ResponseEntity::ok);
    }
}
