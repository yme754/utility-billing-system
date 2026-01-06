package com.utility.payment.entity;

import java.time.LocalDateTime;

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
@Document(collection = "payments")
public class Payment {
	@Id
	private String id;
	private String billId;
	private Double amount;
	private String paymentMode;
	private String status;
	private String transactionId;
	private LocalDateTime paymentDate;
}
