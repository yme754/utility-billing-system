package com.utility.billing.dto;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
	private String to;
    private String subject;
    private String body;    
    private boolean isInvoice; 
    private String billId;
    private Double amount;
}
