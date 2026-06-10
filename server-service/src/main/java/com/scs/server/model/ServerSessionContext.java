package com.scs.server.model;

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
public class ServerSessionContext {

    private String sessionId;
    private String userId;
    private String serverId;
    private String encryptedSessionKeyForServer;
    private SecretKey sessionKey;
    private Instant createdAt;
}
