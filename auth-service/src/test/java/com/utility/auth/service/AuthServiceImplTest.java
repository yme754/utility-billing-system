package com.utility.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.utility.auth.dto.AuthRequest;
import com.utility.auth.entity.User;
import com.utility.auth.repository.UserRepository;
import com.utility.auth.util.JwtUtil;
import com.utility.notification.dto.EmailRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AuthServiceImplTest {
	@Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_success() {
        User user = User.builder().username("u1").email("e1").phoneNumber("p1").password("raw").build();
        when(userRepo.existsByUsername("u1")).thenReturn(Mono.just(false));
        when(userRepo.existsByEmail("e1")).thenReturn(Mono.just(false));
        when(userRepo.existsByPhoneNumber("p1")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("raw")).thenReturn("encoded");
        when(userRepo.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(authService.register(user))
                .expectNextMatches(saved -> saved.getPassword().equals("encoded"))
                .verifyComplete();

        verify(kafkaTemplate, atLeastOnce()).send(eq("notification-topic"), any(EmailRequest.class));
    }

    @Test
    void register_conflictUsername() {
        User user = User.builder().username("u1").email("e1").phoneNumber("p1").build();
        when(userRepo.existsByUsername("u1")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(user))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void register_conflictEmail() {
        User user = User.builder().username("u1").email("e1").phoneNumber("p1").build();
        when(userRepo.existsByUsername("u1")).thenReturn(Mono.just(false));
        when(userRepo.existsByEmail("e1")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(user))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void register_conflictPhone() {
        User user = User.builder().username("u1").email("e1").phoneNumber("p1").build();
        when(userRepo.existsByUsername("u1")).thenReturn(Mono.just(false));
        when(userRepo.existsByEmail("e1")).thenReturn(Mono.just(false));
        when(userRepo.existsByPhoneNumber("p1")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(user))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void login_success() {
        AuthRequest req = AuthRequest.builder().username("u1").password("pw").build();
        User user = User.builder().id("id1").username("u1").password("encoded").roles(List.of("ROLE_USER")).status("ACTIVE").build();

        when(userRepo.findByUsername("u1")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("pw", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt");

        StepVerifier.create(authService.login(req))
                .expectNextMatches(resp -> resp.getAccessToken().equals("jwt"))
                .verifyComplete();
    }

    @Test
    void login_invalidCredentials() {
        AuthRequest req = AuthRequest.builder().username("u1").password("bad").build();
        User user = User.builder().username("u1").password("encoded").build();

        when(userRepo.findByUsername("u1")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("bad", "encoded")).thenReturn(false);

        StepVerifier.create(authService.login(req))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void createStaff_missingRoles() {
        AuthRequest req = AuthRequest.builder().username("u1").email("e1").password("pw").build();
        StepVerifier.create(authService.createStaff(req, null))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void createStaff_success() {
        AuthRequest req = AuthRequest.builder().username("u1").email("e1").password("pw").roles(List.of("ROLE_STAFF")).build();
        when(userRepo.findByUsername("u1")).thenReturn(Mono.empty());
        when(userRepo.findByEmail("e1")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        User saved = User.builder().id("id1").username("u1").email("e1").roles(List.of("ROLE_STAFF")).build();
        when(userRepo.save(any(User.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(authService.createStaff(req, null))
                .expectNextMatches(resp -> resp.getMessage().contains("User created successfully"))
                .verifyComplete();
    }

    @Test
    void approveUser_success() {
        User user = User.builder().id("id1").username("u1").email("e1").build();
        when(userRepo.findById("id1")).thenReturn(Mono.just(user));
        when(userRepo.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(authService.approveUser("id1", "ADMIN"))
                .expectNext("User approved successfully")
                .verifyComplete();

        verify(kafkaTemplate).send(eq("notification-topic"), any(EmailRequest.class));
    }

    @Test
    void updateUserStatus_success() {
        User user = User.builder().id("id1").username("u1").status("PENDING").build();
        when(userRepo.findById("id1")).thenReturn(Mono.just(user));
        when(userRepo.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(authService.updateUserStatus("id1", "ACTIVE"))
                .expectNext("User status updated to ACTIVE")
                .verifyComplete();
    }

    @Test
    void updateUserStatus_notFound() {
        when(userRepo.findById("id1")).thenReturn(Mono.empty());

        StepVerifier.create(authService.updateUserStatus("id1", "ACTIVE"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void updatePassword_success() {
        User user = User.builder().id("id1").username("u1").password("encoded").build();
        when(userRepo.findByUsername("u1")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("newEncoded");
        when(userRepo.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(authService.updatePassword("u1", "old", "new"))
                .verifyComplete();
    }

    @Test
    void updatePassword_wrongCurrent() {
        User user = User.builder().id("id1").username("u1").password("encoded").build();
        when(userRepo.findByUsername("u1")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        StepVerifier.create(authService.updatePassword("u1", "wrong", "new"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void getAllUsers_success() {
        User user = User.builder().id("id1").username("u1").email("e1").roles(List.of("ROLE_USER")).status("ACTIVE").build();
        when(userRepo.findAll()).thenReturn(Flux.just(user));

        StepVerifier.create(authService.getAllUsers())
                .expectNextMatches(dto -> dto.getUsername().equals("u1"))
                .verifyComplete();
    }

    @Test
    void findByUsername_success() {
        User user = User.builder().id("id1").username("u1").build();
        when(userRepo.findByUsername("u1")).thenReturn(Mono.just(user));

        StepVerifier.create(authService.findByUsername("u1"))
                .expectNext(user)
                .verifyComplete();
    }
}