package com.utility.auth.service;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;

import reactor.core.publisher.Mono;

public interface AuthService {
	Mono<AuthResponse> register(AuthRequest request);
	Mono<AuthResponse> login(AuthRequest request);
	Mono<AuthResponse> createStaff(AuthRequest request, String roleName);
}