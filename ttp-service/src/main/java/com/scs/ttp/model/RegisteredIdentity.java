package com.scs.ttp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredIdentity {

    private String identityId;
    private String identityName;
    private IdentityType type;
    private PublicKey publicKey;
    private X509Certificate certificate;
    private Instant registeredAt;
    private Instant certificateExpiresAt;
}
