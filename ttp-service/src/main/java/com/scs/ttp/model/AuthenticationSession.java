package com.scs.ttp.model;

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
public class AuthenticationSession {

    private String sessionId;
    private String userId;
    private String serverId;
    private SecretKey aesSessionKey;
    private Instant createdAt;
    private boolean authenticated;
}
