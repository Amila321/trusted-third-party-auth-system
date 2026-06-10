package com.scs.clientbackend.exception;

public class ClientAuthenticationException extends RuntimeException {

    public ClientAuthenticationException(String message) {
        super(message);
    }

    public ClientAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
