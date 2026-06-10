package com.scs.server.service;

import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.hash.HashService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.IdentityRegistrationRequest;
import com.scs.dto.auth.IdentityStatusResponse;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.dto.auth.TtpPublicKeyResponse;
import com.scs.server.exception.ServerConfigurationException;
import com.scs.server.model.ServerIdentityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerRegistrationService {

    private final TtpClient ttpClient;
    private final RsaKeyService rsaKeyService;
    private final RsaEncryptionService rsaEncryptionService;
    private final EncodingService encodingService;
    private final HashService hashService;
    private final InMemoryServerIdentityStore identityStore;

    public IdentityStatusResponse register(IdentityRegistrationRequest request) {
        try {
            TtpPublicKeyResponse ttpPublicKey = ttpClient.getTtpPublicKey();
            PublicKey ttpKey = rsaKeyService.decodePublicKeyPem(ttpPublicKey.getPublicKeyPem());
            KeyPair serverKeyPair = rsaKeyService.generateKeyPair();
            String publicKeyPem = rsaKeyService.encodePublicKeyPem(serverKeyPair.getPublic());
            String identityId = hashService.hashIdentity(normalize(request.getIdentityName()));
            String encryptedIdentityId = encodingService.encodeBase64(
                    rsaEncryptionService.encrypt(identityId.getBytes(StandardCharsets.UTF_8), ttpKey)
            );

            RegistrationResponse registration = ttpClient.registerServer(RegistrationRequest.builder()
                    .identityName(request.getIdentityName())
                    .encryptedIdentityId(encryptedIdentityId)
                    .publicKeyPem(publicKeyPem)
                    .build());

            ServerIdentityContext context = ServerIdentityContext.builder()
                    .identityId(registration.getIdentityId())
                    .identityName(request.getIdentityName())
                    .keyPair(serverKeyPair)
                    .publicKeyPem(publicKeyPem)
                    .certificatePem(registration.getCertificatePem())
                    .registeredAt(registration.getRegisteredAt())
                    .build();
            identityStore.save(context);
            log.info("Registered server identity at TTP identityId={}", registration.getIdentityId());
            return toResponse(context);
        } catch (Exception e) {
            throw new ServerConfigurationException("Failed to register server identity at TTP: " + e.getMessage());
        }
    }

    public IdentityStatusResponse currentIdentity() {
        return identityStore.current()
                .map(this::toResponse)
                .orElseGet(() -> IdentityStatusResponse.builder().registered(false).build());
    }

    private IdentityStatusResponse toResponse(ServerIdentityContext context) {
        return IdentityStatusResponse.builder()
                .identityId(context.getIdentityId())
                .identityName(context.getIdentityName())
                .certificatePem(context.getCertificatePem())
                .publicKeyPem(context.getPublicKeyPem())
                .registeredAt(context.getRegisteredAt())
                .registered(true)
                .build();
    }

    private String normalize(String identityName) {
        return identityName.trim().toLowerCase(Locale.ROOT);
    }
}
