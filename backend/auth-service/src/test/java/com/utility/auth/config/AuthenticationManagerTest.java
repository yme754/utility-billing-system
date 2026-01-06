package com.utility.auth.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.utility.auth.util.JwtUtil;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AuthenticationManagerTest {
    @Mock
    private JwtUtil jwtUtil;

    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        authenticationManager = new AuthenticationManager(jwtUtil);
    }

    @Test
    void authenticate_validToken_returnsAuthentication() {
        String token = "validToken";
        Authentication auth = new UsernamePasswordAuthenticationToken("ignored", token);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user1");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_USER"));
        when(jwtUtil.getAllClaimsFromToken(token)).thenReturn(claims);

        Mono<Authentication> result = authenticationManager.authenticate(auth);

        StepVerifier.create(result)
                .expectNextMatches(a -> a.getName().equals("user1") &&
                        a.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_USER")))
                .verifyComplete();

        verify(jwtUtil).validateToken(token);
        verify(jwtUtil).getAllClaimsFromToken(token);
    }

    @Test
    void authenticate_invalidToken_returnsEmpty() {
        String token = "badToken";
        Authentication auth = new UsernamePasswordAuthenticationToken("ignored", token);

        when(jwtUtil.validateToken(token)).thenReturn(false);

        Mono<Authentication> result = authenticationManager.authenticate(auth);

        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil).validateToken(token);
        verify(jwtUtil, never()).getAllClaimsFromToken(token);
    }
}