package com.scs.clientbackend.controller;

import com.scs.clientbackend.exception.ClientAuthenticationException;
import com.scs.clientbackend.exception.DataExchangeException;
import com.scs.clientbackend.exception.ServerConnectionException;
import com.scs.clientbackend.exception.TtpClientException;
import com.scs.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ClientExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", e.getMessage());
    }

    @ExceptionHandler(TtpClientException.class)
    public ResponseEntity<ErrorResponse> handleTtpClient(TtpClientException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "TTP_UNAVAILABLE", e.getMessage(), null);
    }

    @ExceptionHandler(ClientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleClientAuthentication(ClientAuthenticationException e) {
        return error(HttpStatus.BAD_REQUEST, "CLIENT_AUTHENTICATION_FAILED", e.getMessage(), null);
    }

    @ExceptionHandler(DataExchangeException.class)
    public ResponseEntity<ErrorResponse> handleDataExchange(DataExchangeException e) {
        return error(HttpStatus.BAD_REQUEST, "DATA_EXCHANGE_FAILED", e.getMessage(), null);
    }

    @ExceptionHandler(ServerConnectionException.class)
    public ResponseEntity<ErrorResponse> handleServerConnection(ServerConnectionException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "SERVER_UNAVAILABLE", e.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, String details) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .errorCode(code)
                .message(message)
                .details(details)
                .timestamp(Instant.now().toString())
                .build());
    }
}
