package com.utility.billing.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.utility.billing.dto.MeterReadingDTO;
import com.utility.billing.dto.TariffDTO;
import com.utility.billing.entity.Bill;
import com.utility.billing.repository.BillRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BillingService {
	private final WebClient.Builder webClientBuilder;
	private final BillRepository billRepo;
	public Mono<Bill> generateBill(String connectionId, String meterId, String utilityName) {
		Mono<MeterReadingDTO> readingMono = webClientBuilder.build().get()
				.uri("http://METER-SERVICE/readings/" + meterId).retrieve()
				.bodyToFlux(MeterReadingDTO.class).last();
		Mono<TariffDTO> tariffMono = webClientBuilder.build().get()
				.uri("http://UTILITY-SERVICE/utilities/tariffs?type=" + utilityName)
				.retrieve().bodyToFlux(TariffDTO.class).next();
		
		return Mono.zip(readingMono, tariffMono).flatMap(tuple-> {
			MeterReadingDTO meterReading = tuple.getT1();
			TariffDTO tariff = tuple.getT2();
			double units = meterReading.getUnitsConsumed();
			double rate = tariff.getSlabs().stream()
					.filter(slab-> units >= slab.getMinUnits() && units<= slab.getMaxUnits())
					.findFirst()
					.map(TariffDTO.Slab::getRatePerUnit).orElse(0.0);
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
	public Mono<Bill> getBill(String billId) {
		return billRepo.findById(billId);
	}
}
