package com.utility.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
	
    @Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    	return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)                
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)                
                .authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/auth/login", "/auth/register", "/auth/validate").permitAll()
                    .pathMatchers("/actuator/**").permitAll()                    
                    .anyExchange().authenticated())
                .build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
