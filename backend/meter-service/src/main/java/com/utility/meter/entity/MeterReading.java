package com.utility.meter.entity;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "readings")
@CompoundIndex(def = "{'meterId':1, 'date': 1}", unique= true)
public class MeterReading {
	@Id
	private String id;
    private String meterId;
    private String connectionId;
    private LocalDate date;
    private Double reading;
    private Double unitsConsumed;
    private String submittedBy;
}
