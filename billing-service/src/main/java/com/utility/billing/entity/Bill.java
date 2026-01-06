package com.utility.billing.entity;

import java.time.LocalDate;
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
@Document(collection = "bills")
public class Bill {
	@Id
    private String id;
    private String connectionId;
    private String meterId;
    private LocalDate billingDate;
    private LocalDate dueDate;    
    private Double previousReading;
    private Double currentReading;
    private Double unitsConsumed;
    private Double ratePerUnit;
    private Double fixedCharge;
    private Double taxAmount;
    private Double amount;
    private Double totalAmount;
    private String status;
    private Double fineAmount; 
    private Double lateFeePerDay; 
    private Integer gracePeriod;
    private LocalDateTime lastReminderSent;
    private String utilityType;
    private String tariffPlanName;
    private String paymentMode;
    private LocalDateTime paymentDate;
}
