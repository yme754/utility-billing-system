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

    @PutMapping("/{id}")
    public Mono<ResponseEntity<TariffPlan>> updateTariff(@PathVariable String id, @RequestBody TariffPlan plan) {
        return tariffRepo.findById(id)
                .flatMap(existing -> {
                    existing.setPlanName(plan.getPlanName());
                    existing.setDescription(plan.getDescription());
                    existing.setUtilityType(plan.getUtilityType());
                    existing.setCategory(plan.getCategory());                    
                    existing.setBillingType(plan.getBillingType());
                    existing.setBaseRate(plan.getBaseRate());
                    existing.setFixedCharge(plan.getFixedCharge());
                    existing.setSlabs(plan.getSlabs());
                    return tariffRepo.save(existing);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteTariff(@PathVariable String id) {
        return tariffRepo.deleteById(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
}