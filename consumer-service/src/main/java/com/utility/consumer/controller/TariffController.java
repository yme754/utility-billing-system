package com.utility.consumer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.utility.consumer.entity.TariffPlan;
import com.utility.consumer.repository.TariffPlanRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/consumers/tariffs")
@RequiredArgsConstructor
public class TariffController {    
    private final TariffPlanRepository tariffRepo;

    @PostMapping
    public Mono<ResponseEntity<TariffPlan>> createTariff(@RequestBody TariffPlan plan) {
        return tariffRepo.save(plan).map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<TariffPlan> getTariffs(@RequestParam(name = "type", required = false) String type) {
        if (type != null) {
            return tariffRepo.findByUtilityType(type);
        }
        return tariffRepo.findAll();
    }
}