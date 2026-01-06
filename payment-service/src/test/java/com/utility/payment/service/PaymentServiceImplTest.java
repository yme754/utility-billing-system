package com.utility.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.reactive.function.client.WebClient;

import com.utility.payment.dto.PaymentRequest;
import com.utility.payment.entity.Payment;
import com.utility.payment.repository.PaymentRepository;
import com.utility.payment.service.PaymentServiceImpl.BillDTO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

	@Mock
    private PaymentRepository paymentRepo;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
    }

    private void mockWebClientResponse(BillDTO responseDto) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BillDTO.class)).thenReturn(Mono.just(responseDto));
    }

    private void mockWebClientError(Throwable error) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BillDTO.class)).thenReturn(Mono.error(error));
    }

    @Test
    void testProcessPayment_Success() {
        String token = "Bearer token";
        PaymentRequest request = new PaymentRequest();
        request.setBillId("BILL-123");
        request.setPaymentMode("UPI");
        BillDTO billDTO = new BillDTO();
        billDTO.setId("BILL-123");
        billDTO.setStatus("PENDING");
        billDTO.setTotalAmount(100.0);
        Payment savedPayment = Payment.builder()
                .billId("BILL-123")
                .amount(100.0)
                .transactionId("TXN-abc")
                .build();

        mockWebClientResponse(billDTO);
        when(paymentRepo.save(any(Payment.class))).thenReturn(Mono.just(savedPayment));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        StepVerifier.create(paymentService.processPayment(request, token))
                .expectNextMatches(p -> 
                    p.getAmount().equals(100.0) && 
                    p.getBillId().equals("BILL-123")
                )
                .verifyComplete();
        verify(paymentRepo, times(1)).save(any(Payment.class));
        verify(kafkaTemplate, times(2)).send(anyString(), any());
        verify(kafkaTemplate).send("payment-success", savedPayment);
        verify(kafkaTemplate).send(eq("notification-topic"), any());
    }

    @Test
    void testProcessPayment_BillAlreadyPaid() {
        String token = "Bearer token";
        PaymentRequest request = new PaymentRequest();
        request.setBillId("BILL-123");
        BillDTO billDTO = new BillDTO();
        billDTO.setId("BILL-123");
        billDTO.setStatus("PAID");
        mockWebClientResponse(billDTO);
        StepVerifier.create(paymentService.processPayment(request, token))
                .expectErrorMessage("This bill is already PAID!")
                .verify();
        verify(paymentRepo, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void testProcessPayment_WebClientError() {
        String token = "Bearer token";
        PaymentRequest request = new PaymentRequest();
        request.setBillId("BILL-123");
        mockWebClientError(new RuntimeException("Billing Service Down"));
        StepVerifier.create(paymentService.processPayment(request, token))
                .expectErrorMessage("Billing Service Down")
                .verify();
    }

    @Test
    void testGetSuccessfulPayments() {
        Payment p1 = Payment.builder().status("SUCCESS").amount(100.0).build();
        Payment p2 = Payment.builder().status("SUCCESS").amount(200.0).build();
        when(paymentRepo.findByStatus("SUCCESS")).thenReturn(Flux.just(p1, p2));
        StepVerifier.create(paymentService.getSuccessfulPayments())
                .expectNext(p1)
                .expectNext(p2)
                .verifyComplete();
    }

    @Test
    void testGetAccountStats_WithData() {
        Payment p1 = Payment.builder().amount(100.0).status("SUCCESS").build();
        Payment p2 = Payment.builder().amount(200.0).status("SUCCESS").build();
        Payment pToday = Payment.builder().amount(50.0).status("SUCCESS").build();
        when(paymentRepo.findByStatus("SUCCESS")).thenReturn(Flux.just(p1, p2));
        when(paymentRepo.findByPaymentDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.just(pToday));
        StepVerifier.create(paymentService.getAccountStats())
                .assertNext(stats -> {
                    assertEquals(300.0, stats.get("totalRevenue"));
                    assertEquals(50.0, stats.get("todayRevenue"));
                })
                .verifyComplete();
    }

    @Test
    void testGetAccountStats_EmptyData() {
        when(paymentRepo.findByStatus("SUCCESS")).thenReturn(Flux.empty());
        when(paymentRepo.findByPaymentDateBetween(any(), any())).thenReturn(Flux.empty());
        StepVerifier.create(paymentService.getAccountStats())
                .assertNext(stats -> {
                    assertEquals(0.0, stats.get("totalRevenue"));
                    assertEquals(0.0, stats.get("todayRevenue"));
                })
                .verifyComplete();
    }
    
    @Test
    void testInnerClassDTOs() {
        PaymentServiceImpl.BillDTO bill1 = new PaymentServiceImpl.BillDTO();
        bill1.setId("123");
        bill1.setTotalAmount(500.0);
        bill1.setStatus("PAID");
        PaymentServiceImpl.BillDTO bill2 = new PaymentServiceImpl.BillDTO();
        bill2.setId("123");
        bill2.setTotalAmount(500.0);
        bill2.setStatus("PAID");
        PaymentServiceImpl.BillDTO bill3 = new PaymentServiceImpl.BillDTO();
        bill3.setId("999");
        assertEquals("123", bill1.getId());
        assertEquals(500.0, bill1.getTotalAmount());
        assertEquals("PAID", bill1.getStatus());
        assertEquals(bill1, bill2);
        assertEquals(bill1.hashCode(), bill2.hashCode());
        assertNotEquals(bill1, bill3);
        assertNotNull(bill1.toString());
        PaymentServiceImpl.EmailRequest email1 = PaymentServiceImpl.EmailRequest.builder()
                .to("test@mail.com")
                .subject("Subject")
                .body("Body")
                .isInvoice(true)
                .billId("B-1")
                .amount(100.0)
                .build();
        PaymentServiceImpl.EmailRequest email2 = new PaymentServiceImpl.EmailRequest();
        email2.setTo("test@mail.com");
        email2.setSubject("Subject");
        email2.setBody("Body");
        email2.setInvoice(true);
        email2.setBillId("B-1");
        email2.setAmount(100.0);
        assertEquals("test@mail.com", email1.getTo());
        assertEquals("Subject", email1.getSubject());
        assertEquals("Body", email1.getBody());
        assertEquals(true, email1.isInvoice());
        assertEquals("B-1", email1.getBillId());
        assertEquals(100.0, email1.getAmount());
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
        assertNotNull(email1.toString());
    }
}