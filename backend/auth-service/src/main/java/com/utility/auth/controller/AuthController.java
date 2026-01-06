package com.utility.auth.controller;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.dto.UserDto;
import com.utility.auth.entity.User;
import com.utility.auth.service.AuthService;
import com.utility.auth.util.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;
    private final JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody AuthRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .build();
        return authService.register(user)
                .map(savedUser -> ResponseEntity.ok(
                        AuthResponse.builder()
                                .message("Registration successful")
                                .userId(savedUser.getId())
                                .build()
                ))
                .onErrorResume(ResponseStatusException.class, e -> Mono.just(
                        ResponseEntity.status(e.getStatusCode())
                        .body(AuthResponse.builder().message(e.getReason()).build())
                ));
    }
    
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return authService.login(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(401).body(
                        AuthResponse.builder().message(e.getMessage()).build()
                )));
    }
    
    @GetMapping("/validate")
    public Mono<String> validateToken(@RequestParam("token") String token) {
    	boolean isValid = Boolean.TRUE.equals(jwtUtil.validateToken(token));
        return Mono.just(isValid ? "VALID" : "INVALID");
    }
    
    @PostMapping("/create-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<AuthResponse>> createStaff(@RequestBody AuthRequest request) {      
        return authService.createStaff(request, null).map(ResponseEntity::ok);
    }
    
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<UserDto> getAllUsers() {
        return authService.getAllUsers();
    }

    @PutMapping("/admin/users/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<AuthResponse>> approveUser(
            @PathVariable String id, 
            @RequestBody Map<String, String> requestBody) {
        
        String role = requestBody.get("role");
        return authService.approveUser(id, role)
                .map(msg -> ResponseEntity.ok(AuthResponse.builder().message(msg).build()));
    }

    @PutMapping("/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<AuthResponse>> updateUserStatus(
            @PathVariable String id, 
            @RequestBody Map<String, String> requestBody) {
        
        String status = requestBody.get("status");
        return authService.updateUserStatus(id, status)
                .map(msg -> ResponseEntity.ok(AuthResponse.builder().message(msg).build()));
    }
    
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<AuthResponse>> changePassword(
            @RequestBody Map<String, String> passwords, 
            Principal principal) {
        
        String currentPassword = passwords.get("currentPassword");
        String newPassword = passwords.get("newPassword");
        String username = principal.getName();

        return authService.updatePassword(username, currentPassword, newPassword)
                .then(Mono.just(ResponseEntity.ok(
                    AuthResponse.builder().message("Password changed successfully").build()
                )))
                .onErrorResume(e -> Mono.just(
                    ResponseEntity.badRequest().body(
                        AuthResponse.builder().message(e.getMessage()).build()
                    )
                ));
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<AuthResponse> handleValidationErrors(WebExchangeBindException ex) {
        String errorMsg = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(AuthResponse.builder()
                .message(errorMsg).build());
    }
}