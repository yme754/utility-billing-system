package com.utility.billing.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.utility.billing.dto.MeterReadingDTO;
import com.utility.billing.dto.TariffDTO;
import com.utility.billing.entity.Bill;
import com.utility.billing.repository.BillRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableReactiveMethodSecurity
public class BillingServiceImpl implements BillingService{
	private final WebClient.Builder webClientBuilder;
	private final BillRepository billRepo;
	
	@Override
	public Mono<Bill> generateBill(String connectionId, String meterId, String utilityName) {
		log.info("Generating bill for Meter: {}, Utility: {}", meterId, utilityName);
		Mono<MeterReadingDTO> readingMono = webClientBuilder.build().get()
				.uri("http://METER-SERVICE/readings/" + meterId).retrieve()
				.bodyToFlux(MeterReadingDTO.class)
				.last()
				.switchIfEmpty(Mono.error(
				        new ResponseStatusException(HttpStatus.NOT_FOUND,
				        		"Meter reading not found for: " + meterId)
				));
		Mono<TariffDTO> tariffMono = webClientBuilder.build().get()
				.uri("http://UTILITY-SERVICE/utilities/tariffs?type=" + utilityName)
				.retrieve()
				.bodyToFlux(TariffDTO.class)
				.next()
				.switchIfEmpty(Mono.error(
				        new ResponseStatusException(HttpStatus.NOT_FOUND,
				                "No Tariff found for utility: " + utilityName)
				));		
		return Mono.zip(readingMono, tariffMono).flatMap(tuple-> {
			MeterReadingDTO meterReading = tuple.getT1();
			TariffDTO tariff = tuple.getT2();
			double units = meterReading.getUnitsConsumed();
			double rate = tariff.getSlabs().stream()
					.filter(slab-> units >= slab.getMinUnits() && units<= slab.getMaxUnits())
					.findFirst()
					.map(TariffDTO.Slab::getRatePerUnit)
					.orElseThrow(() -> new ResponseStatusException(
					        HttpStatus.BAD_REQUEST,
					        "No slab found for units: " + units
					));
			double energyCharge = units*rate;
			double tax = (energyCharge + tariff.getFixedCharge())*(tariff.getTaxPercentage()/100);
			double total = energyCharge + tariff.getFixedCharge()+ tax;
			Bill bill = Bill.builder().connectionId(connectionId).meterId(meterId)
					.billingDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(15))
					.unitsConsumed(units).ratePerUnit(rate).fixedCharge(tariff.getFixedCharge())
					.taxAmount(tax).amount(energyCharge).totalAmount(total).status("UNPAID").build();
			return billRepo.save(bill);
		});
	}
	
	@Override
	public Mono<Bill> getBill(String billId) {
		return billRepo.findById(billId);
	}

	@Override
	public Mono<Void> updateBillStatus(String id, String status) {
		return billRepo.findById(id)
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found")))
				.flatMap(bill -> {
					bill.setStatus(status);
					return billRepo.save(bill);
				}).then();
	}
}
