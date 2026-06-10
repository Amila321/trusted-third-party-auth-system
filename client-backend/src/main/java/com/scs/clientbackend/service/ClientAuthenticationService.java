package com.scs.clientbackend.service;

import com.scs.clientbackend.dto.ClientAuthenticationInitiateRequest;
import com.scs.clientbackend.dto.ClientAuthenticationStateResponse;
import com.scs.clientbackend.dto.ClientSessionStatusResponse;
import com.scs.clientbackend.exception.ClientAuthenticationException;
import com.scs.clientbackend.exception.TtpClientException;
import com.scs.clientbackend.model.ClientIdentityContext;
import com.scs.clientbackend.model.ClientSessionContext;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.dto.session.SessionKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAuthenticationService {

    private final RsaKeyService rsaKeyService;
    private final RsaEncryptionService rsaEncryptionService;
    private final AesKeyService aesKeyService;
    private final EncodingService encodingService;
    private final TtpClient ttpClient;
    private final InMemoryClientAuthenticationStore authenticationStore;

    public ClientAuthenticationStateResponse initiate(ClientAuthenticationInitiateRequest request) {
        try {
            KeyPair keyPair = rsaKeyService.generateKeyPair();
            String publicKeyPem = rsaKeyService.encodePublicKeyPem(keyPair.getPublic());
            RegistrationResponse registrationResponse = ttpClient.registerUser(RegistrationRequest.builder()
                    .identityName(request.getIdentityName())
                    .publicKeyPem(publicKeyPem)
                    .build());

            ClientIdentityContext identityContext = ClientIdentityContext.builder()
                    .identityId(registrationResponse.getIdentityId())
                    .identityName(request.getIdentityName())
                    .keyPair(keyPair)
                    .publicKeyPem(publicKeyPem)
                    .certificatePem(registrationResponse.getCertificatePem())
                    .registeredAt(registrationResponse.getRegisteredAt())
                    .build();
            authenticationStore.saveIdentity(identityContext);
            log.info("Stored client identity identityId={}", registrationResponse.getIdentityId());

            return ClientAuthenticationStateResponse.builder()
                    .identityId(registrationResponse.getIdentityId())
                    .certificatePem(registrationResponse.getCertificatePem())
                    .publicKeyPem(publicKeyPem)
                    .registeredAt(registrationResponse.getRegisteredAt())
                    .build();
        } catch (TtpClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientAuthenticationException("Failed to initiate client authentication", e);
        }
    }

    public ClientSessionStatusResponse complete(SessionKeyResponse response) {
        ClientIdentityContext identityContext = authenticationStore.findActiveIdentity().orElse(null);
        if (identityContext == null) {
            throw new ClientAuthenticationException("No client identity is available for session completion");
        }

        try {
            byte[] encryptedKey = encodingService.decodeBase64(response.getEncryptedSessionKey());
            byte[] sessionKeyBytes = rsaEncryptionService.decrypt(encryptedKey, identityContext.getKeyPair().getPrivate());
            SecretKey sessionKey = aesKeyService.decodeKey(encodingService.encodeBase64(sessionKeyBytes));

            ClientSessionContext sessionContext = ClientSessionContext.builder()
                    .sessionId(response.getSessionId())
                    .identityId(identityContext.getIdentityId())
                    .sessionKey(sessionKey)
                    .establishedAt(Instant.now())
                    .build();
            authenticationStore.saveSession(sessionContext);
            log.info("Stored client session sessionId={} identityId={}", response.getSessionId(), identityContext.getIdentityId());

            return ClientSessionStatusResponse.builder()
                    .sessionId(response.getSessionId())
                    .identityId(identityContext.getIdentityId())
                    .sessionKeyBase64(aesKeyService.encodeKey(sessionKey))
                    .authenticated(true)
                    .build();
        } catch (Exception e) {
            throw new ClientAuthenticationException("Failed to complete client authentication", e);
        }
    }
}
