package com.utility.billing.dto;

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
    private String consumerName;
    private String utilityType;
    private String tariffCategory;
    private String meterNumber;
    private LocalDate connectionDate;
    private String status;
}
