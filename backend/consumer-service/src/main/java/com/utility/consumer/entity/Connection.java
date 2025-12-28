package com.utility.consumer.entity;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "connections")
public class Connection {
	@Id
	private String id;
	@Indexed
	private String consumerId;
	private String utilityType;
	@Indexed(unique = true)
	private String meterNumber;
	private String tariffCategory;
	private LocalDate connectionDate;
	private String status;
}
