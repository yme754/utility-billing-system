package com.utility.auth.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SecurityContextRepositoryTest {

    @Mock
    private AuthenticationManager authenticationManager;

    private SecurityContextRepository repository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new SecurityContextRepository(authenticationManager);
    }

    @Test
    void load_withBearerToken_returnsSecurityContext() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer goodtoken")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        Authentication auth = new UsernamePasswordAuthenticationToken("user1", "goodtoken");
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(auth));
        Mono<SecurityContext> result = repository.load(exchange);
        StepVerifier.create(result)
                .expectNextMatches(ctx -> ctx.getAuthentication().getName().equals("user1"))
                .verifyComplete();
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void load_withoutHeader_returnsEmpty() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        Mono<SecurityContext> result = repository.load(exchange);
        StepVerifier.create(result).verifyComplete();
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void save_throwsUnsupportedOperation() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        SecurityContext context = mock(SecurityContext.class);
        assertThrows(UnsupportedOperationException.class, () -> {
            repository.save(exchange, context).block();
        }, "Should throw UnsupportedOperationException as saving context is not supported in this stateless implementation");
    }
}