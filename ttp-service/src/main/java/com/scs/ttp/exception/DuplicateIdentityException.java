package com.scs.ttp.exception;

public class DuplicateIdentityException extends RuntimeException {

    public DuplicateIdentityException(String identityName) {
        super("Identity already registered: " + identityName);
    }
}
