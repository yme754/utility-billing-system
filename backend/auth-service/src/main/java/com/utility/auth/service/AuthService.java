package com.utility.auth.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.entity.User;
import com.utility.auth.repository.UserRepository;
import com.utility.auth.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	
	public Mono<AuthResponse> register(AuthRequest request) {
		return userRepo.findByUsername(request.getUsername())
				.flatMap(existing -> Mono.error(new RuntimeException("User already exists")))
				.switchIfEmpty(Mono.defer(() -> {
					User newUser = User.builder().username(request.getUsername())
							.email(request.getEmail()).password(passwordEncoder.encode(request.getPassword()))
							.roles(List.of("ROLE_CONSUMER")).active(true).build();
					return userRepo.save(newUser);
				}))
				.cast(User.class).map(savedUser ->
				AuthResponse.builder().userId(savedUser.getId())
				.message("User registered successfully").role("ROLE_CONSUMER").build());
	}
	
	public Mono<AuthResponse> login(AuthRequest request) {
		return userRepo.findByUsername(request.getUsername())
				.filter(u-> passwordEncoder.matches(request.getPassword(), u.getPassword()))
				.map(user-> AuthResponse.builder()
						.accessToken(jwtUtil.generateToken(user))
						.userId(user.getId()).role(user.getRoles().get(0))
						.message("Login successful").build())
				.switchIfEmpty(Mono.error(new RuntimeException("Invalid Credentials")));
	}
}
