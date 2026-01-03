package com.utility.auth.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.dto.UserDto;
import com.utility.auth.entity.User;
import com.utility.auth.repository.UserRepository;
import com.utility.auth.util.JwtUtil;
import com.utility.common.event.UserRegisteredEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.utility.notification.dto.EmailRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService{
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	
	@Override
    public Mono<AuthResponse> register(AuthRequest request) {
        return userRepo.save(User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .roles(List.of("ROLE_USER"))
                .active(false)
                .status("PENDING")
                .build())
                .flatMap(user -> {
                    sendRegistrationNotifications(user);
                    return Mono.just(AuthResponse.builder()
                            .message("Registration successful. Awaiting approval.")
                            .build());
                });
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
	
	@Override
    public Flux<UserDto> getAllUsers() {
        return userRepo.findAll()
                .map(this::mapToDto);
    }

    @Override
    public Mono<String> approveUser(String id, String role) {
        return userRepo.findById(id)
        		.flatMap(user -> {
                    user.setRoles(List.of(role.startsWith("ROLE_") ? role : "ROLE_" + role));
                    user.setStatus("ACTIVE");
                    user.setActive(true);
                    return userRepo.save(user);
                })
                .doOnSuccess(user -> {
                    EmailRequest approvalMail = EmailRequest.builder()
                            .to(user.getEmail())
                            .subject("Account Approved - Utilix")
                            .body("Great news " + user.getUsername() + "!\n\nYour account has been approved as " + user.getRoles().get(0) + 
                                  ". You can now access all features.\n\nLogin now: http://localhost:4200/login")
                            .build();
                    kafkaTemplate.send("notification-topic", approvalMail);
                })
                .map(savedUser -> "User approved successfully");
    }

    @Override
    public Mono<String> updateUserStatus(String id, String status) {
        return userRepo.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    user.setStatus(status);                    
                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        user.setActive(true);
                    } else {
                        user.setActive(false);
                    }
                    return userRepo.save(user);
                })
                .map(savedUser -> "User status updated to " + status);
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .status(user.getStatus() != null ? user.getStatus() : "PENDING")
                .build();
    }
    
    private void sendRegistrationNotifications(User user) {
        EmailRequest userMail = EmailRequest.builder()
                .to(user.getEmail())
                .subject("Registration Received - Utilix")
                .body("Hello " + user.getUsername() + ",\n\nYour registration is confirmed. " +
                      "You will be notified once the admin approves your account.\n\n" +
                      "Login here: http://localhost:4200/login")
                .build();

        EmailRequest adminMail = EmailRequest.builder()
                .to("admin@utility.com")
                .subject("New User Alert: " + user.getUsername())
                .body("A new user has registered: " + user.getUsername() + " (" + user.getEmail() + ").\n\n" +
                      "Review here: http://localhost:4200/admin/manage-users")
                .build();

        kafkaTemplate.send("notification-topic", userMail);
        kafkaTemplate.send("notification-topic", adminMail);
        log.info("Kafka events published for user: {}", user.getUsername());
    }

}