package com.scs.clientbackend.exception;

public class DataExchangeException extends RuntimeException {

    public DataExchangeException(String message) {
        super(message);
    }

    public DataExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
