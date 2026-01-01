package com.utility.auth.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.entity.User;
import com.utility.auth.repository.UserRepository;
import com.utility.auth.util.JwtUtil;
import com.utility.common.event.UserRegisteredEvent;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	
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
                    return userRepo.save(newUser)
                            .flatMap(savedUser -> {
                                UserRegisteredEvent event = new UserRegisteredEvent(
                                    savedUser.getId(),
                                    savedUser.getUsername(),
                                    savedUser.getEmail(),
                                    request.getFirstName() != null ? request.getFirstName() : "User",
                                    request.getLastName() != null ? request.getLastName() : "Consumer",
                                    request.getAddress() != null ? request.getAddress() : "Not Provided"
                                );
                                return Mono.fromFuture(kafkaTemplate.send("user-registered", event))
                                           .thenReturn(savedUser);
                            });
                }))
                .cast(User.class)
                .map(savedUser -> AuthResponse.builder().userId(savedUser.getId())
                        .message("User registered and profile creation queued").role("ROLE_CONSUMER").build());
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
		List<String> rolesToAssign = request.getRoles();
		if(rolesToAssign == null || rolesToAssign.isEmpty())
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles must be provided in body"));
		return userRepo.findByUsername(request.getUsername())
				.flatMap(existing-> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists")))
				.switchIfEmpty(userRepo.findByEmail(request.getEmail())
                        .flatMap(existing -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists")))
                )
				.switchIfEmpty(Mono.defer(() -> {
                    User staffUser = User.builder()
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .roles(rolesToAssign)
                            .active(true)
                            .build();
                    return userRepo.save(staffUser);
                }))
				.cast(User.class)
				.map(savedUser -> AuthResponse.builder()
                        .userId(savedUser.getId())
                        .message("User created successfully with roles: " + rolesToAssign)
                        .role(rolesToAssign.get(0))
                        .build());
    }
}
