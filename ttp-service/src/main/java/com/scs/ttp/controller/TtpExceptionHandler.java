package com.scs.ttp.controller;

import com.scs.dto.common.ErrorResponse;
import com.scs.ttp.exception.DuplicateIdentityException;
import com.scs.ttp.exception.IdentityNotFoundException;
import com.scs.ttp.exception.TtpOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class TtpExceptionHandler {

    @ExceptionHandler(DuplicateIdentityException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIdentity(DuplicateIdentityException e) {
        log.warn("Registration rejected: {}", e.getMessage());
        return error(HttpStatus.CONFLICT, "DUPLICATE_IDENTITY", e.getMessage(), null);
    }

    @ExceptionHandler(IdentityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIdentityNotFound(IdentityNotFoundException e) {
        log.warn("Certificate lookup failed: {}", e.getMessage());
        return error(HttpStatus.NOT_FOUND, "IDENTITY_NOT_FOUND", e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", e.getMessage());
    }

    @ExceptionHandler(TtpOperationException.class)
    public ResponseEntity<ErrorResponse> handleTtpOperation(TtpOperationException e) {
        log.error("TTP operation failed", e);
        return error(HttpStatus.BAD_REQUEST, "TTP_OPERATION_FAILED", e.getMessage(), null);
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
