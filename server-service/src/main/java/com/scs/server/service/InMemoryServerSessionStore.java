package com.scs.server.service;

import com.scs.server.model.ServerSessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class InMemoryServerSessionStore {

    private final ConcurrentMap<String, ServerSessionContext> sessionsById = new ConcurrentHashMap<>();

    public void save(ServerSessionContext sessionContext) {
        sessionsById.put(sessionContext.getSessionId(), sessionContext);
        log.info("Stored server session context sessionId={} userId={} serverId={}",
                sessionContext.getSessionId(),
                sessionContext.getUserId(),
                sessionContext.getServerId());
    }

    public Optional<ServerSessionContext> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    public void close(String sessionId) {
        sessionsById.remove(sessionId);
        log.info("Closed server session context sessionId={}", sessionId);
    }

    public int size() {
        return sessionsById.size();
    }
}
