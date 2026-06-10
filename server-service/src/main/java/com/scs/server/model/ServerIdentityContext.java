package com.scs.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.KeyPair;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerIdentityContext {

    private String identityId;
    private String identityName;
    private KeyPair keyPair;
    private String publicKeyPem;
    private String certificatePem;
    private String registeredAt;
}
