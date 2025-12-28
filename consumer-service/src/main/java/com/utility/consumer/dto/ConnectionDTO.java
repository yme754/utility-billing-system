package com.utility.consumer.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDTO {
	private String id;
	private String consumerId;
	private String utilityType;
	private String tarrifCategory;
	private String meterNumber;
	private LocalDate connectionDate;
	private String status;
}
