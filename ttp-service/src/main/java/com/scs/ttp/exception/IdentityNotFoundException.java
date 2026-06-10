package com.scs.ttp.exception;

public class IdentityNotFoundException extends RuntimeException {

    public IdentityNotFoundException(String identityId) {
        super("Identity not found: " + identityId);
    }
}
