package com.utility.utility.service;

import com.utility.utility.entity.Tariff;
import com.utility.utility.entity.Utility;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UtilityService {
	public Flux<Utility> getAllUtilities();
	public Mono<Tariff> addTariff(Tariff tariff);
	public Flux<Tariff> getTariffsByutility(String utilityName);
}
