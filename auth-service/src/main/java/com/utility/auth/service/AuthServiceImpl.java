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
    
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String NOTIFICATION_TOPIC = "notification-topic";
    private static final String ADMIN_EMAIL = "yxsh2999@gmail.com";
    
    @Override
    public Mono<User> register(User user) {
    	return userRepo.existsByUsername(user.getUsername())
                .flatMap(usernameExists -> {
                    if (Boolean.TRUE.equals(usernameExists)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken"));
                    }
                    return userRepo.existsByEmail(user.getEmail());
                })
                .flatMap(emailExists -> {
                    if (Boolean.TRUE.equals(emailExists)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered"));
                    }
                    return userRepo.existsByPhoneNumber(user.getPhoneNumber());
                })
                .flatMap(phoneExists -> {
                    if (Boolean.TRUE.equals(phoneExists)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already registered"));
                    }
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    user.setRoles(List.of("ROLE_CONSUMER"));
                    user.setActive(false);
                    user.setStatus(STATUS_PENDING);
                    return userRepo.save(user);
                })
                .doOnSuccess(this::sendRegistrationNotifications);
        }
    
    @Override
    public Mono<AuthResponse> login(AuthRequest request) {
        return userRepo.findByUsername(request.getUsername())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .map(user -> AuthResponse.builder()
                        .accessToken(jwtUtil.generateToken(user))
                        .userId(user.getId())
                        .role(user.getRoles().get(0))
                        .status(user.getStatus())
                        .message("Login successful")
                        .build())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials")));
    }
    
    @Override
    public Mono<AuthResponse> createStaff(AuthRequest request, String roleName) {
        List<String> rolesToAssign = request.getRoles();
        if(rolesToAssign == null || rolesToAssign.isEmpty())
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles must be provided in body"));
        return userRepo.findByUsername(request.getUsername())
                .flatMap(existing -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists")))
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
                            .status(STATUS_ACTIVE) 
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
                    user.setStatus(STATUS_ACTIVE);
                    user.setActive(true);
                    return userRepo.save(user);
                })
                .doOnSuccess(user -> {
                    EmailRequest approvalMail = EmailRequest.builder()
                            .to(ADMIN_EMAIL)
                            .subject("Account Approved - Utilix")
                            .body("Great news " + user.getUsername() + "!\n\nYour account has been approved as " + user.getRoles().get(0) + 
                                  ". You can now access all features.\n\nLogin now: http://localhost:4200/login")
                            .build();
                    kafkaTemplate.send(NOTIFICATION_TOPIC, approvalMail);
                })
                .map(savedUser -> "User approved successfully");
    }

    @Override
    public Mono<String> updateUserStatus(String id, String status) {
    	return userRepo.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    user.setStatus(status);
                    user.setActive(STATUS_ACTIVE.equalsIgnoreCase(status));
                    return userRepo.save(user);
                })
                .map(savedUser -> "User status updated to " + status);
    }
    
    @Override
    public Mono<Void> updatePassword(String username, String currentPassword, String newPassword) {
        return userRepo.findByUsername(username)
                .flatMap(user -> {
                    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                        return Mono.error(new RuntimeException("Current password does not match"));
                    }
                    user.setPassword(passwordEncoder.encode(newPassword));
                    return userRepo.save(user);
                }).then();
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .status(user.getStatus() != null ? user.getStatus() : STATUS_PENDING)
                .build();
    }
    
    private void sendRegistrationNotifications(User user) {
        EmailRequest userMail = EmailRequest.builder()
                .to(ADMIN_EMAIL) 
                .subject("Registration Received - Utilix")
                .body("Hello " + user.getUsername() + ",\n\nYour registration is confirmed. " +
                      "You will be notified once the admin approves your account.\n\n" +
                      "Login here: http://localhost:4200/login")
                .build();
        EmailRequest adminMail = EmailRequest.builder()
                .to(ADMIN_EMAIL) 
                .subject("New User Alert: " + user.getUsername())
                .body("A new user has registered: " + user.getUsername() + " (" + user.getEmail() + ").\n\n" +
                      "Review here: http://localhost:4200/admin/manage-users")
                .build();
        kafkaTemplate.send(NOTIFICATION_TOPIC, userMail);
        kafkaTemplate.send(NOTIFICATION_TOPIC, adminMail);
        log.info("Kafka events published: 1 to User ({}), 1 to Admin", user.getEmail());
    }
    
    @Override
    public Mono<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }
}