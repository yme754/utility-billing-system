package com.utility.consumer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConnectionApprovalDTO {
	@NotBlank(message = "Meter Number is required")
    private String meterNumber;
}
