package com.utility.auth.service;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.dto.UserDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuthService {
	Mono<AuthResponse> register(AuthRequest request);
	Mono<AuthResponse> login(AuthRequest request);
	Mono<AuthResponse> createStaff(AuthRequest request, String roleName);
	Flux<UserDto> getAllUsers();
	Mono<String> approveUser(String id, String role);
	Mono<String> updateUserStatus(String id, String status);
}