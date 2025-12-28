package com.utility.auth.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.utility.auth.entity.User;
import com.utility.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner{
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	
	public void run(String... args) throws Exception {
		userRepo.findByUsername("admin")
		.switchIfEmpty(createAdmin()).subscribe();
	}
	private reactor.core.publisher.Mono<User> createAdmin() {
		User admin = User.builder().username("admin")
				.email("admin@utility.com")
				.password(passwordEncoder.encode("Admin@12345"))
				.roles(List.of("ROLE_ADMIN")).active(true).build();
		System.out.println("ADMIN CREATED(Username: admin | Password: Admin@12345)");
		return userRepo.save(admin);
	}
}
