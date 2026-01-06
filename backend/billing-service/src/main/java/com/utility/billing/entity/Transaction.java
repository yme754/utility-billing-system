package com.utility.billing.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "transactions")
public class Transaction {
	@Id
    private String id;
    private String billId;
    private Double amount;
    private String paymentMode;
    private String transactionReference;
    private LocalDateTime timestamp;
    private String status;
}
