package com.utility.billing.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MeterReadingEvent {
	private String readingId;
    private String connectionId;
    private String meterId;
    private Double unitsConsumed;
    private LocalDate readingDate;
}
