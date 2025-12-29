package com.utility.billing.dto;

import lombok.Data;

@Data
public class MeterReadingDTO {
	private Double unitsConsumed;
	private String meterId;
}
