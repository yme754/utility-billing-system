package com.utility.auth.controller;

import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.service.AuthService;
import com.utility.auth.util.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;
	private final JwtUtil jwtUtil;
	
	@PostMapping("/register")
	public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody AuthRequest request) {
		return authService.register(request)
				.map(response-> ResponseEntity.ok(response))
				.onErrorResume(ResponseStatusException.class, e -> Mono.just(
						ResponseEntity.status(e.getStatusCode())
						.body(AuthResponse.builder().message(e.getReason()).build())
						))
				.onErrorResume(WebExchangeBindException.class, e -> {
					String errors = e.getBindingResult().getAllErrors().stream()
							.map(DefaultMessageSourceResolvable::getDefaultMessage)
							.collect(Collectors.joining(", "));
					return Mono.just(ResponseEntity.badRequest().body(AuthResponse.builder()
							.message(errors).build()));
				})
				.onErrorResume(Exception.class, e -> Mono.just(
						ResponseEntity.internalServerError().body(AuthResponse.builder()
						.message("An unexpected error occured").build()))
						);
	}
	
	@PostMapping("/login")
	public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
		return authService.login(request)
				.map(response -> ResponseEntity.ok(response))
				.onErrorResume(e-> Mono.just(ResponseEntity.status(401).body(
						AuthResponse.builder().message(e.getMessage()).build()
						)));
	}
	
	@GetMapping("/validate")
	public Mono<String> validateToken(@RequestParam("token") String token) {
		if(jwtUtil.validateToken(token)) 
			return Mono.just("VALID");
		else return Mono.just("INVALID");
	}
}
