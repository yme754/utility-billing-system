package com.utility.utility.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.utility.entity.Tariff;
import com.utility.utility.entity.Utility;
import com.utility.utility.repository.TariffRepository;
import com.utility.utility.repository.UtilityRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class UtilityServiceImpl implements UtilityService{
	private final UtilityRepository utilityRepo;
	private final TariffRepository tariffRepo;
	
	@Override
	public Flux<Utility> getAllUtilities() {
		return utilityRepo.findAll();
	}
	
	@Override
	public Mono<Tariff> addTariff(Tariff tariff) {
		return tariffRepo.findByName(tariff.getName())
                .flatMap(existing -> Mono.<Tariff>error(new ResponseStatusException(HttpStatus.CONFLICT, "Tariff with this name already exists")))
                .switchIfEmpty(tariffRepo.save(tariff));
	}
	
	@Override
	public Flux<Tariff> getTariffsByutility(String utilityName) {
		return utilityRepo.findByName(utilityName)
				.flatMapMany(u-> tariffRepo.findByUtilityId(u.getId()));
	}
}
