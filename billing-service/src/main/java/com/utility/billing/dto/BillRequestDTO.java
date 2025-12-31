package com.utility.billing.dto;

import lombok.Data;

@Data
public class BillRequestDTO {
	private String connectionId;
    private String meterId;
    private String utilityName;
}
