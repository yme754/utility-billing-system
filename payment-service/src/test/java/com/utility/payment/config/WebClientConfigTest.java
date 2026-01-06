package com.utility.payment.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class WebClientConfigTest {
	@Test
    void webClientBuilder_returnsBuilder() {
        WebClientConfig config = new WebClientConfig();
        WebClient.Builder builder = config.webClientBuilder();
        assertNotNull(builder);
    }
}