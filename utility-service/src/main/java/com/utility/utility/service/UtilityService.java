package com.utility.utility.service;

import com.utility.utility.entity.Tariff;
import com.utility.utility.entity.Utility;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UtilityService {
	Flux<Utility> getAllUtilities();
	Mono<Tariff> addTariff(Tariff tariff);
	Flux<Tariff> getTariffsByutility(String utilityName);
}
