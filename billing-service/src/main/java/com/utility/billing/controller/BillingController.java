package com.utility.billing.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utility.billing.dto.BillRequestDTO;
import com.utility.billing.entity.Bill;
import com.utility.billing.service.BillingService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillingController {
	private final BillingService billingService;
	
	@PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER')")
    public Mono<ResponseEntity<Bill>> generateBill(@RequestBody BillRequestDTO request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return billingService.generateBill(
                request.getConnectionId(), 
                request.getMeterId(), 
                request.getUtilityName(), 
                token
        ).map(ResponseEntity::ok);
    }
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'BILLING_OFFICER', 'ACCOUNTS_OFFICER', 'CONSUMER')")
	public Mono<ResponseEntity<Bill>> getBill(@PathVariable String id) {
		return billingService.getBill(id).map(ResponseEntity::ok);
	}
	
	@PutMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS_OFFICER', 'CONSUMER')")
	public Mono<ResponseEntity<Void>> updateBillStatus(@PathVariable String id, @RequestParam String status) {
		return billingService.updateBillStatus(id, status)
                .map(v -> ResponseEntity.ok().<Void>build());
	}
	
	@GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ACCOUNTS_OFFICER', 'ADMIN')")
    public Mono<ResponseEntity<Flux<Bill>>> getPendingBills() {
        return Mono.just(ResponseEntity.ok(billingService.getPendingBills()));
    }

    @GetMapping("/my-bills/{connectionId}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'ADMIN')")
    public Mono<ResponseEntity<Flux<Bill>>> getMyBills(@PathVariable String connectionId) {
        return Mono.just(ResponseEntity.ok(billingService.getBillsByConnection(connectionId)));
    }
    
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('CONSUMER')")
    public Mono<ResponseEntity<Void>> payBill(@PathVariable String id, @RequestParam String mode) {
        return billingService.payBill(id, mode).map(v -> ResponseEntity.ok().<Void>build());
    }
}
