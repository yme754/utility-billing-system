package com.utility.auth.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {
	@NotBlank(message = "Username is required")
    @Size(min = 3, message = "Username must be at least 3 characters")
	private String username;
	@NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
	private String email;
	@NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
	private String password;
	private List<String> roles;
	private String firstName;
    private String lastName;
    private String address;
    private String phoneNumber;
}