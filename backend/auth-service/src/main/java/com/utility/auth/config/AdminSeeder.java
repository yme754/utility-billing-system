package com.utility.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.utility.auth.entity.User;
import com.utility.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner{
	private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
    	userRepo.findByUsername(adminUsername)
        .switchIfEmpty(Mono.defer(this::createAdmin))
        .doOnNext(user -> log.info("System check: Admin user '{}' is ready.", user.getUsername()))
        .block(); 
    }

    private Mono<User> createAdmin() {
        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .roles(List.of("ROLE_ADMIN"))
                .active(true)
                .build();

        return userRepo.save(admin)
                .doOnSuccess(u -> log.info("Successfully created default admin user: {}", adminUsername));
    }
}