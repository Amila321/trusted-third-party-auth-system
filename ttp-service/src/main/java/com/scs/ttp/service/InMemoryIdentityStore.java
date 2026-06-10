package com.scs.ttp.service;

import com.scs.ttp.exception.DuplicateIdentityException;
import com.scs.ttp.exception.IdentityNotFoundException;
import com.scs.ttp.model.IdentityType;
import com.scs.ttp.model.RegisteredIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class InMemoryIdentityStore {

    private final Map<String, RegisteredIdentity> identitiesById = new ConcurrentHashMap<>();
    private final Map<String, String> identityIdsByNormalizedName = new ConcurrentHashMap<>();

    public synchronized RegisteredIdentity registerIdentity(
            String name,
            String identityId,
            PublicKey key,
            IdentityType type,
            X509Certificate certificate
    ) {
        String normalizedName = normalizeName(name);
        if (identityIdsByNormalizedName.containsKey(normalizedName)) {
            log.warn("Rejected duplicate registration attempt for identityName={}", name);
            throw new DuplicateIdentityException(name);
        }
        if (identitiesById.containsKey(identityId)) {
            log.warn("Rejected duplicate registration attempt for identityId={}", identityId);
            throw new DuplicateIdentityException(identityId);
        }

        Instant registeredAt = Instant.now();
        RegisteredIdentity identity = RegisteredIdentity.builder()
                .identityId(identityId)
                .identityName(name.trim())
                .type(type)
                .publicKey(key)
                .certificate(certificate)
                .registeredAt(registeredAt)
                .certificateExpiresAt(certificate.getNotAfter().toInstant())
                .build();

        identitiesById.put(identityId, identity);
        identityIdsByNormalizedName.put(normalizedName, identityId);
        log.info("Registered identity id={} name={} type={}", identityId, identity.getIdentityName(), type);
        return identity;
    }

    public RegisteredIdentity getIdentity(String identityId) {
        RegisteredIdentity identity = identitiesById.get(identityId);
        if (identity == null) {
            throw new IdentityNotFoundException(identityId);
        }
        return identity;
    }

    public boolean identityExists(String name) {
        return identityIdsByNormalizedName.containsKey(normalizeName(name));
    }

    public boolean identityIdExists(String identityId) {
        return identitiesById.containsKey(identityId);
    }

    public String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
