package com.utility.utility.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.utility.utility.entity.Utility;
import com.utility.utility.repository.UtilityRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class UtilitySeeder implements CommandLineRunner{
	private final UtilityRepository utilityRepo;
	
	public void run(String... args) {
		utilityRepo.count().filter(count-> count == 0)
		.flatMapMany(c-> Flux.just(
				Utility.builder().name("ELECTRICITY")
				.unitOfMeasure("kWh").active(true).build(),
				Utility.builder().name("WATER")
				.unitOfMeasure("Liters").active(true).build(),
				Utility.builder().name("GAS")
				.unitOfMeasure("Cubic Feet").active(true).build(),
				Utility.builder().name("INTERNET")
				.unitOfMeasure("GB").active(true).build()
				))
		.flatMap(utilityRepo::save).subscribe(u->
		System.out.println("Seeded Utility: "+ u.getName()));
	}
}