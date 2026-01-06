package com.utility.meter.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SecurityContextRepositoryTest {

    private AuthenticationManager authManager;
    private SecurityContextRepository repo;

    @BeforeEach
    void setup() {
        authManager = mock(AuthenticationManager.class);
        repo = new SecurityContextRepository(authManager);
    }

    @Test
    void load_withBearerHeader_returnsContext() {
        String token = "abc123";
        Authentication auth = new UsernamePasswordAuthenticationToken("user", token);
        when(authManager.authenticate(any())).thenReturn(Mono.just(auth));

        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(repo.load(exchange))
                .expectNextMatches(ctx -> ctx.getAuthentication().getName().equals("user"))
                .verifyComplete();
    }

    @Test
    void load_withoutHeader_returnsEmpty() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(repo.load(exchange))
                .verifyComplete();
    }
}