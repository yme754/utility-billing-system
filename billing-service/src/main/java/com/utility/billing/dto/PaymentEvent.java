package com.utility.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
	private String id;
    private String billId;
    private Double amount;
    private String paymentMode;
    private String status;
    private String transactionId;
}
