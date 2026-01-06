package com.utility.auth.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import com.utility.auth.dto.ErrorResponse;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrors_returnsBadRequest() {
        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("field", "must not be blank")));
        when(ex.getBindingResult()).thenReturn(bindingResult);
        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("must not be blank"));
        assertEquals("Validation Failed", response.getBody().getError());
    }

    @Test
    void handleServiceException_returnsStatusFromException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Username taken");

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username taken", response.getBody().getMessage());
        assertTrue(response.getBody().getError().contains("409"));
    }

    @Test
    void handleGenericException_returnsInternalServerError() {
        Exception ex = new Exception("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Something went wrong", response.getBody().getMessage());
        assertEquals("Internal Server Error", response.getBody().getError());
    }
}
