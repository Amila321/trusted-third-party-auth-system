package com.scs.clientbackend.exception;

public class ServerConnectionException extends RuntimeException {

    public ServerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerConnectionException(String message) {
        super(message);
    }
}
