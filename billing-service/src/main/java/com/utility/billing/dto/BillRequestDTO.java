package com.utility.billing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillRequestDTO {
	private String connectionId;
    private String meterId;
    private String utilityName;
    private Double units;
}
