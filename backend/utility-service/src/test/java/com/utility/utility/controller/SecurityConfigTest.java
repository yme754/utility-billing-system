package com.utility.utility.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import com.utility.utility.config.AuthenticationManager;
import com.utility.utility.config.SecurityContextRepository;

class SecurityConfigTest {
    @Test
    void securityWebFilterChain_createsBean() {
        AuthenticationManager authManager = mock(AuthenticationManager.class);
        SecurityContextRepository repo = mock(SecurityContextRepository.class);
        SecurityConfig config = new SecurityConfig(authManager, repo);

        assertNotNull(config.securityWebFilterChain(ServerHttpSecurity.http()));
    }
}
