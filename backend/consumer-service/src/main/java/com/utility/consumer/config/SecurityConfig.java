package com.utility.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
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
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)                
                .authorizeExchange(exchanges -> exchanges
                		.pathMatchers(HttpMethod.GET, "/consumers/tariffs").permitAll()                 	    
                	    .pathMatchers(HttpMethod.GET, "/consumers/count").permitAll()
                	    .pathMatchers(HttpMethod.POST, "/consumers/profile").permitAll() 
                        .pathMatchers("/consumers/**").permitAll() 
                        .anyExchange().authenticated()
                )
                .build();
    }
}
