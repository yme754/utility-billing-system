package com.utility.consumer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utility.consumer.entity.TariffPlan;
import com.utility.consumer.service.TariffService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
public class TariffController {
	private final TariffService tariffService;

    @PostMapping
    public Mono<ResponseEntity<TariffPlan>> createTariff(@RequestBody TariffPlan plan) {
        return tariffService.addPlan(plan).map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<TariffPlan> getAllTariffs() {
        return tariffService.getAllPlans();
    }
}
