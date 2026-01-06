package com.utility.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.utility.notification.dto.EmailRequest;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

	@Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@utilix.com");
    }
    @Test
    void testSendEmail_SimpleText_Success() {
        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .subject("Welcome")
                .body("Hello User")
                .isInvoice(false)
                .build();

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailService.sendEmail(request);
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendEmail_Invoice_Success() {
        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .subject("Bill")
                .isInvoice(true)
                .billId("BILL-123")
                .amount(500.0)
                .body("Usage Details")
                .build();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailService.sendEmail(request);
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendEmail_Invoice_WithNullValues() {
        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .subject("Bill")
                .isInvoice(true)
                .billId(null)
                .amount(null)
                .body(null)
                .build();

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmail(request);

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendEmail_Retry_SuccessOnSecondAttempt() {
        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .subject("Retry Test")
                .body("This body is required to prevent crash")
                .isInvoice(false)
                .build();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Network Error"))
            .doNothing()
            .when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(request);
        verify(javaMailSender, times(2)).send(mimeMessage);
    }

    @Test
    void testSendEmail_Retry_FailOnSecondAttempt() {
        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .subject("Retry Fail Test")
                .body("This body is required to prevent crash")
                .isInvoice(false)
                .build();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Attempt 1 Fail"))
            .doThrow(new MailSendException("Attempt 2 Fail"))
            .when(javaMailSender).send(any(MimeMessage.class));
        emailService.sendEmail(request);
        verify(javaMailSender, times(2)).send(mimeMessage);
    }
    
    @Test
    void testSendEmail_FatalConstructionError() {
        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .build();
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("Message Creation Failed"));
        emailService.sendEmail(request);
        verify(javaMailSender, times(0)).send(any(MimeMessage.class));
    }
}