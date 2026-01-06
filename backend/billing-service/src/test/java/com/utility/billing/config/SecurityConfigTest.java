package com.utility.billing.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

class SecurityConfigTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Test
    void securityWebFilterChain_buildsSuccessfully() {
        MockitoAnnotations.openMocks(this);
        SecurityConfig config = new SecurityConfig(authenticationManager, securityContextRepository);

        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain chain = config.securityWebFilterChain(http);

        assertNotNull(chain);
    }
}