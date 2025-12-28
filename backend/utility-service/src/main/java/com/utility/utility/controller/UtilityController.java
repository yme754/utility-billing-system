package com.utility.utility.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utility.utility.entity.Tariff;
import com.utility.utility.entity.Utility;
import com.utility.utility.service.UtilityService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/utilities")
@RequiredArgsConstructor
public class UtilityController {
	private final UtilityService utilityService;
	
	@GetMapping
	public Flux<Utility> getAllUtilities() {
		return utilityService.getAllUtilities();
	}
	
	@PostMapping("/tariffs")
	public Mono<Tariff> createTariff(@RequestBody Tariff tariff) {
		return utilityService.addTariff(tariff);
	}
	
	@GetMapping("/tariffs")
	public Flux<Tariff> getTariffs(@RequestParam String type) {
		return utilityService.getTariffsByutility(type);
	}
}
