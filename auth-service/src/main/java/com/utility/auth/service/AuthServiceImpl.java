package com.utility.auth.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.entity.User;
import com.utility.auth.repository.UserRepository;
import com.utility.auth.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	
	@Override
	public Mono<AuthResponse> register(AuthRequest request) {
		return userRepo.findByUsername(request.getUsername())
				.flatMap(existing -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "User already exists")))
				.switchIfEmpty(Mono.defer(() -> 
					userRepo.findByEmail(request.getEmail())
						.flatMap(existing -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists")))
				))
				.switchIfEmpty(Mono.defer(() -> {
					User newUser = User.builder()
							.username(request.getUsername())
							.email(request.getEmail())
							.password(passwordEncoder.encode(request.getPassword()))
							.roles(List.of("ROLE_CONSUMER"))
							.active(true)
							.build();
					return userRepo.save(newUser);
				}))
				.cast(User.class)
				.map(savedUser -> AuthResponse.builder()
						.userId(savedUser.getId())
						.message("User registered successfully")
						.role("ROLE_CONSUMER")
						.build());
	}
	
	@Override
	public Mono<AuthResponse> login(AuthRequest request) {
		return userRepo.findByUsername(request.getUsername())
				.filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
				.map(user -> AuthResponse.builder().accessToken(jwtUtil.generateToken(user))
						.userId(user.getId()).role(user.getRoles().get(0))
						.message("Login successful").build())
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials")));
	}
	
	@Override
	public Mono<AuthResponse> createStaff(AuthRequest request, String roleName) {
		List<String> allowedRoles = List.of("ROLE_BILLING_OFFICER", "ROLE_ACCOUNTS_OFFICER");
		if(!allowedRoles.contains(roleName))
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role"));
		return userRepo.findByUsername(request.getUsername())
				.flatMap(existing-> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists")))
				.switchIfEmpty(userRepo.findByEmail(request.getEmail())
                        .flatMap(existing -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists")))
                )
				.switchIfEmpty(Mono.defer(() -> {
					User staffUser = User.builder().username(request.getUsername())
							.email(request.getEmail()).password(passwordEncoder.encode(request.getPassword()))
							.roles(List.of(roleName)).active(true).build();
					return userRepo.save(staffUser);
				}))
				.cast(User.class)
				.map(savedUser-> AuthResponse.builder()
						.userId(savedUser.getId())
						.message("Staff created successfully: "+ roleName)
						.role(roleName).build());
	}
}
