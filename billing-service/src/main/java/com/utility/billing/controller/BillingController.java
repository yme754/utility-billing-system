package com.utility.billing.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utility.billing.entity.Bill;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.service.BillingService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillingController {
	private final BillingService billingService;
	@Autowired
	private BillRepository billRepo;
	
	@PostMapping("/generate")
	public Mono<ResponseEntity<Bill>> generateBill(@RequestParam String connectionId,
			@RequestParam String meterId, @RequestParam String utiltyName) {
		return billingService.generateBill(connectionId, meterId, utiltyName).map(ResponseEntity::ok);
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Bill>> getBill(@PathVariable String id) {
		return billingService.getBill(id).map(ResponseEntity::ok);
	}
	
	@PutMapping("/{id}/status")
	public Mono<ResponseEntity<Void>> updateBillStatus(@PathVariable String id, @RequestParam String status) {
		return billRepo.findById(id).flatMap(bill -> {
			bill.setStatus(status);
			return billRepo.save(bill);
		})
				.map(updated -> ResponseEntity.ok().<Void>build())
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
}
