package com.scs.server.controller;

import com.scs.dto.common.ErrorResponse;
import com.scs.server.exception.DataExchangeException;
import com.scs.server.exception.ServerConfigurationException;
import com.scs.server.exception.SessionDecisionException;
import com.scs.server.exception.TtpClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class ServerExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", e.getMessage());
    }

    @ExceptionHandler(ServerConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleServerConfiguration(ServerConfigurationException e) {
        log.error("Server authentication configuration error: {}", e.getMessage());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_CONFIGURATION_ERROR", e.getMessage(), null);
    }

    @ExceptionHandler(TtpClientException.class)
    public ResponseEntity<ErrorResponse> handleTtpClient(TtpClientException e) {
        log.warn("TTP authentication request failed: {}", e.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "TTP_UNAVAILABLE", e.getMessage(), null);
    }

    @ExceptionHandler(SessionDecisionException.class)
    public ResponseEntity<ErrorResponse> handleSessionDecision(SessionDecisionException e) {
        log.warn("Invalid authenticated TTP decision: {}", e.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "INVALID_TTP_DECISION", e.getMessage(), null);
    }

    @ExceptionHandler(DataExchangeException.class)
    public ResponseEntity<ErrorResponse> handleDataExchange(DataExchangeException e) {
        log.warn("Encrypted data exchange failed: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, "DATA_EXCHANGE_FAILED", e.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, String details) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(code)
                .message(message)
                .details(details)
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
