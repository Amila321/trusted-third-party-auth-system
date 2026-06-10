package com.scs.clientbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSessionContext {

    private String sessionId;
    private String identityId;
    private SecretKey sessionKey;
    private Instant establishedAt;
}
