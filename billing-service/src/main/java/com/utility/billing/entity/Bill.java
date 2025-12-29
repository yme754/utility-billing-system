package com.utility.billing.entity;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bills")
public class Bill {
	@Id
	private String id;
	private String connectionId;
	private String meterId;
	private LocalDate billingDate;
	private LocalDate dueDate;
	private Double ratePerUnit;
	private Double fixedCharge;
	private Double amount;
	private Double taxAmount;
	private Double totalAmount;
	private Double unitsConsumed;
	private String status;
}
