package com.utility.notification.controller;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.utility.notification.dto.EmailRequest;
import com.utility.notification.service.EmailService;

import reactor.test.StepVerifier;

class NotificationControllerTest {

    @Mock
    private EmailService service;

    private NotificationController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new NotificationController(service);
    }

    @Test
    void sendTestEmail_invokesServiceAndReturnsMono() {
        EmailRequest request = new EmailRequest();
        controller.sendTestEmail(request).block();
        verify(service).sendEmail(request);

        StepVerifier.create(controller.sendTestEmail(request))
                .expectNext("Email request processed")
                .verifyComplete();
    }
}