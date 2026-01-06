package com.utility.consumer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
	@NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
	private String email;
	private String address;
    private String profileImageUrl;
}
