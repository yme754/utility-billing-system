package com.utility.payment.dto;

import lombok.Data;

@Data
public class PaymentRequest {
	private String billId;
    private String paymentMode;
}
