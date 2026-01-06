package com.utility.consumer.config;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.utility.consumer.util.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager{
	private final JwtUtil jwtUtil;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        try {
            if (!jwtUtil.validateToken(authToken)) {
                System.out.println("TOKEN INVALID: Signature verification failed or token expired.");
                return Mono.empty();
            }
            Claims claims = jwtUtil.getAllClaimsFromToken(authToken);
            String username = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);
            System.out.println("TOKEN VALID. User: " + username + " | Roles: " + roles);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            return Mono.just(new UsernamePasswordAuthenticationToken(username, authToken, authorities));
        } catch (Exception e) {
            System.out.println("AUTH EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return Mono.empty();
        }
    }
}