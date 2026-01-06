package com.utility.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.utility.auth.config.TestSecurityConfig;
import com.utility.auth.dto.AuthRequest;
import com.utility.auth.dto.AuthResponse;
import com.utility.auth.dto.UserDto;
import com.utility.auth.service.AuthService;
import com.utility.auth.util.JwtUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
	    "app.admin.username=admin",
	    "app.admin.password=Admin@12345",
	    "app.admin.email=admin@utility.com"
	})
class AuthControllerTest {
	@Autowired
    private WebTestClient webTestClient;
	@MockBean
    private AuthService authService;
	@MockBean
    private JwtUtil jwtUtil;
    
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void register_success() {
        AuthRequest req = AuthRequest.builder().username("user123").password("password123").email("user@example.com").build();
        var saved = com.utility.auth.entity.User.builder().id("id1").username("u1").build();
        when(authService.register(any(com.utility.auth.entity.User.class))).thenReturn(Mono.just(saved));

        webTestClient.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(resp -> resp.getMessage().equals("Registration successful"));
    }

    @Test
    void register_conflict() {
        AuthRequest req = AuthRequest.builder().username("user123").password("password123").email("user@example.com").build();
        when(authService.register(any(com.utility.auth.entity.User.class)))
                .thenReturn(Mono.error(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Username is already taken")));

        webTestClient.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(AuthResponse.class)
                .value(resp -> resp.getMessage().equals("Username is already taken"));
    }

    @Test
    void login_success() {
        AuthRequest req = AuthRequest.builder().username("u1").password("pw").build();
        AuthResponse resp = AuthResponse.builder().accessToken("jwt").message("Login successful").build();
        when(authService.login(req)).thenReturn(Mono.just(resp));

        webTestClient.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(r -> r.getAccessToken().equals("jwt"));
    }

    @Test
    void login_invalid() {
        AuthRequest req = AuthRequest.builder().username("u1").password("bad").build();
        when(authService.login(req)).thenReturn(Mono.error(new RuntimeException("Invalid Credentials")));

        webTestClient.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(AuthResponse.class)
                .value(r -> r.getMessage().equals("Invalid Credentials"));
    }

    @Test
    void validateToken_valid() {
        when(jwtUtil.validateToken("jwt")).thenReturn(true);

        webTestClient.get().uri("/auth/validate?token=jwt")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("VALID");
    }

    @Test
    void validateToken_invalid() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);

        webTestClient.get().uri("/auth/validate?token=bad")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("INVALID");
    }

    @Test
    void createStaff_success() {
        AuthRequest req = AuthRequest.builder().username("staff").email("s@e.com").roles(List.of("ROLE_STAFF")).build();
        AuthResponse resp = AuthResponse.builder().message("User created successfully").build();
        when(authService.createStaff(req, null)).thenReturn(Mono.just(resp));

        webTestClient.post().uri("/auth/create-staff")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(r -> r.getMessage().contains("User created successfully"));
    }

    @Test
    void getAllUsers_success() {
        UserDto dto = UserDto.builder().id("id1").username("u1").email("e1").roles(List.of("ROLE_USER")).build();
        when(authService.getAllUsers()).thenReturn(Flux.just(dto));

        webTestClient.get().uri("/auth/admin/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserDto.class)
                .hasSize(1);
    }

    @Test
    void approveUser_success() {
        when(authService.approveUser("id1", "ADMIN")).thenReturn(Mono.just("User approved successfully"));

        webTestClient.put().uri("/auth/admin/users/id1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("role", "ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(r -> r.getMessage().equals("User approved successfully"));
    }

    @Test
    void updateUserStatus_success() {
        when(authService.updateUserStatus("id1", "ACTIVE")).thenReturn(Mono.just("User status updated to ACTIVE"));

        webTestClient.put().uri("/auth/admin/users/id1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "ACTIVE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(r -> r.getMessage().equals("User status updated to ACTIVE"));
    }

    @Test
    void changePassword_success() {
        when(authService.updatePassword("u1", "old", "new")).thenReturn(Mono.empty());

        webTestClient
            .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                new UsernamePasswordAuthenticationToken("u1", "ignored", AuthorityUtils.NO_AUTHORITIES)))
            .post().uri("/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("currentPassword", "old", "newPassword", "new"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(AuthResponse.class)
            .value(r -> r.getMessage().equals("Password changed successfully"));
    }

    @Test
    void changePassword_error() {
        when(authService.updatePassword("u1", "wrong", "new"))
            .thenReturn(Mono.error(new RuntimeException("Current password does not match")));

        webTestClient
            .mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                new UsernamePasswordAuthenticationToken("u1", "ignored", AuthorityUtils.NO_AUTHORITIES)))
            .post().uri("/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("currentPassword", "wrong", "newPassword", "new"))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(AuthResponse.class)
            .value(r -> r.getMessage().equals("Current password does not match"));
    }

}