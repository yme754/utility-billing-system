package com.utility.utility.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.utility.utility.util.JwtUtil;

import io.jsonwebtoken.Claims;
import reactor.test.StepVerifier;

class AuthenticationManagerTest {

    @Mock
    private JwtUtil jwtUtil;

    private AuthenticationManager manager;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        manager = new AuthenticationManager(jwtUtil);
    }

    @Test
    void authenticate_validToken_returnsAuth() {
        String token = "valid";
        Claims claims = mock(Claims.class);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getAllClaimsFromToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user1");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_ADMIN"));

        Authentication input = new UsernamePasswordAuthenticationToken("user1", token);

        StepVerifier.create(manager.authenticate(input))
                .expectNextMatches(auth -> auth.getName().equals("user1"))
                .verifyComplete();
    }

    @Test
    void authenticate_invalidToken_returnsEmpty() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);
        Authentication input = new UsernamePasswordAuthenticationToken("user", "bad");

        StepVerifier.create(manager.authenticate(input))
                .verifyComplete();
    }
}
