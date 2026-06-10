package com.scs.ttp.service;

import com.scs.ttp.model.AuthenticationSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class InMemorySessionStore {

    private final ConcurrentMap<String, AuthenticationSession> sessionsById = new ConcurrentHashMap<>();

    public AuthenticationSession createSession(String userId, String serverId, SecretKey sessionKey) {
        AuthenticationSession session = AuthenticationSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .serverId(serverId)
                .aesSessionKey(sessionKey)
                .createdAt(Instant.now())
                .authenticated(true)
                .build();
        sessionsById.put(session.getSessionId(), session);
        log.info("Created authentication session sessionId={} userId={} serverId={}",
                session.getSessionId(),
                userId,
                serverId);
        return session;
    }

    public Optional<AuthenticationSession> getSession(String sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    public void closeSession(String sessionId) {
        sessionsById.remove(sessionId);
        log.info("Closed authentication session sessionId={}", sessionId);
    }
}
