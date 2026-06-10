package com.scs.server.service;

import com.scs.server.model.ServerIdentityContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class InMemoryServerIdentityStore {

    private final AtomicReference<ServerIdentityContext> currentIdentity = new AtomicReference<>();

    public void save(ServerIdentityContext identityContext) {
        currentIdentity.set(identityContext);
    }

    public Optional<ServerIdentityContext> current() {
        return Optional.ofNullable(currentIdentity.get());
    }
}
