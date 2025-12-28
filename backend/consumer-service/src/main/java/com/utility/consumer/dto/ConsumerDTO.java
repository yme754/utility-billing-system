package com.utility.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerDTO {
	private String id;
	private String userId;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String email;
	private String address;
}
