package com.utility.billing.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

 class WebClientConfigTest {

    @Test
    void wwebClientBuilder_returnsBuilder() {
        WebClientConfig config = new WebClientConfig();
        WebClient.Builder builder = config.wwebClientBuilder();
        assertNotNull(builder);
    }
}