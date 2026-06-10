package com.scs.clientbackend.service;

import com.scs.clientbackend.model.ClientIdentityContext;
import com.scs.clientbackend.model.ClientSessionContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class InMemoryClientAuthenticationStore {

    private final ConcurrentMap<String, ClientIdentityContext> identitiesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClientSessionContext> sessionsById = new ConcurrentHashMap<>();
    private final AtomicReference<String> activeIdentityId = new AtomicReference<>();

    public void saveIdentity(ClientIdentityContext identityContext) {
        identitiesById.put(identityContext.getIdentityId(), identityContext);
        activeIdentityId.set(identityContext.getIdentityId());
    }

    public Optional<ClientIdentityContext> findIdentity(String identityId) {
        return Optional.ofNullable(identitiesById.get(identityId));
    }

    public Optional<ClientIdentityContext> findActiveIdentity() {
        return Optional.ofNullable(activeIdentityId.get()).flatMap(this::findIdentity);
    }

    public void saveSession(ClientSessionContext sessionContext) {
        sessionsById.put(sessionContext.getSessionId(), sessionContext);
    }

    public Optional<ClientSessionContext> findSession(String sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    public void closeSession(String sessionId) {
        sessionsById.remove(sessionId);
    }
}
