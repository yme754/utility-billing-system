package com.utility.utility.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tariffs")
public class Tariff {
	@Id
	private String id;
	private String utilityId;
	private String name;
	private Double fixedCharge;
	private Double taxPercentage;
	private List<TariffSlab> slabs;
	private boolean active;
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TariffSlab {
		private Integer minUnits;
		private Integer maxUnits;
		private Double ratePerUnit;
	}
}
