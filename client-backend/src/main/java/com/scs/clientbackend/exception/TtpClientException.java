package com.scs.clientbackend.exception;

public class TtpClientException extends RuntimeException {

    public TtpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TtpClientException(String message) {
        super(message);
    }
}
