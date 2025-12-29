package com.utility.billing.dto;

import java.util.List;

import lombok.Data;

@Data
public class TariffDTO {
	private Double fixedCharge;
	private Double taxPercentage;
	private List<Slab> slabs;
	
	@Data
	public static class Slab {
		private Integer minUnits;
		private Integer maxUnits;
		private Double ratePerUnit;
	}
}
