package com.utility.billing.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TariffDTO {
	private String id;
    private String utilityType;
    private String billingType;
    private Double baseRate;
    private Double fixedCharge;
    private Double taxPercentage;
    private Double lateFeePerDay;
    private Integer gracePeriodDays;
    private List<Slab> slabs;
    private String planName;
    private String category;

    @Data
    public static class Slab {
        private Integer minUnits;
        private Integer maxUnits;
        private Double rate; 
        private Double ratePerUnit;
    }
}