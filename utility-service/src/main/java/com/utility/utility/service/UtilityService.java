package com.utility.utility.service;

import org.springframework.stereotype.Service;

import com.utility.utility.entity.Tariff;
import com.utility.utility.entity.Utility;
import com.utility.utility.repository.TariffRepository;
import com.utility.utility.repository.UtilityRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UtilityService {
	private final UtilityRepository utilityRepo;
	private final TariffRepository tariffRepo;
	
	public Flux<Utility> getAllUtilities() {
		return utilityRepo.findAll();
	}
	
	public Mono<Tariff> addTariff(Tariff tariff) {
		return tariffRepo.save(tariff);
	}
	
	public Flux<Tariff> getTariffsByutility(String utilityName) {
		return utilityRepo.findByName(utilityName)
				.flatMapMany(u-> tariffRepo.findByUtilityId(u.getId()));
	}
}
