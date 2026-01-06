package com.utility.auth.filter;

import io.jsonwebtoken.Claims;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;

import com.utility.auth.util.JwtUtil;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WebFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter(jwtUtil);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_validToken_setsAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer goodtoken")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(jwtUtil.validateToken("goodtoken")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user1");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_USER"));
        when(jwtUtil.getAllClaimsFromToken("goodtoken")).thenReturn(claims);

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result.contextWrite(ReactiveSecurityContextHolder.clearContext()))
                .verifyComplete();

        verify(jwtUtil).validateToken("goodtoken");
        verify(jwtUtil).getAllClaimsFromToken("goodtoken");
        verify(chain).filter(exchange);
    }

    @Test
    void filter_invalidToken_skipsAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer badtoken")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("badtoken")).thenReturn(false);

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();

        verify(jwtUtil).validateToken("badtoken");
        verify(chain).filter(exchange);
    }

    @Test
    void filter_noHeader_skipsAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }
}