package com.utility.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
	private String id;
    private String billId;
    private Double amount;
    private String paymentMode;
    private String status;
    private String transactionId;
}
