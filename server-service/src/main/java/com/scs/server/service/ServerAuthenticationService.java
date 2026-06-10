package com.scs.server.service;

import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.server.exception.ServerConfigurationException;
import com.scs.server.exception.SessionDecisionException;
import com.scs.server.model.ServerSessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerAuthenticationService {

    private final TtpClient ttpClient;
    private final InMemoryServerSessionStore sessionStore;
    private final RsaKeyService rsaKeyService;
    private final RsaEncryptionService rsaEncryptionService;
    private final AesKeyService aesKeyService;
    private final EncodingService encodingService;

    @Value("${server.identity.certificate-pem:}")
    private String serverCertificatePem;

    @Value("${server.identity.private-key-pem:}")
    private String serverPrivateKeyPem;

    public TtpAuthenticationDecision handleUserServiceRequest(UserAuthenticationRequest request) {
        validateServerMaterial();
        ServerAuthenticationRequest ttpRequest = ServerAuthenticationRequest.builder()
                .userId(request.getUserId())
                .serverId(request.getServerId())
                .userCertificatePem(request.getUserCertificatePem())
                .serverCertificatePem(serverCertificatePem)
                .challenge(request.getChallenge())
                .signedChallenge(request.getSignedChallenge())
                .build();

        TtpAuthenticationDecision decision = ttpClient.authenticateUserForServer(ttpRequest);
        log.info("Received TTP authentication decision userId={} serverId={} authenticated={}",
                request.getUserId(),
                request.getServerId(),
                decision.isAuthenticated());

        if (decision.isAuthenticated()) {
            storeAuthenticatedSession(request, decision);
        }
        return decision;
    }

    private void storeAuthenticatedSession(UserAuthenticationRequest request, TtpAuthenticationDecision decision) {
        if (!StringUtils.hasText(decision.getSessionId())) {
            throw new SessionDecisionException("Authenticated TTP decision did not include a session ID");
        }
        if (!StringUtils.hasText(decision.getEncryptedSessionKeyForServer())) {
            throw new SessionDecisionException("Authenticated TTP decision did not include the server session key");
        }

        sessionStore.save(ServerSessionContext.builder()
                .sessionId(decision.getSessionId())
                .userId(request.getUserId())
                .serverId(request.getServerId())
                .encryptedSessionKeyForServer(decision.getEncryptedSessionKeyForServer())
                .sessionKey(decryptSessionKeyForServer(decision))
                .createdAt(Instant.now())
                .build());
    }

    private SecretKey decryptSessionKeyForServer(TtpAuthenticationDecision decision) {
        if (!StringUtils.hasText(serverPrivateKeyPem)) {
            log.info("server.identity.private-key-pem is not configured; storing encrypted session key only");
            return null;
        }
        try {
            byte[] encryptedKey = encodingService.decodeBase64(decision.getEncryptedSessionKeyForServer());
            byte[] keyBytes = rsaEncryptionService.decrypt(encryptedKey, rsaKeyService.decodePrivateKeyPem(serverPrivateKeyPem));
            return aesKeyService.decodeKey(encodingService.encodeBase64(keyBytes));
        } catch (Exception e) {
            throw new SessionDecisionException("Failed to decrypt server session key from TTP decision");
        }
    }

    private void validateServerMaterial() {
        if (!StringUtils.hasText(serverCertificatePem)) {
            throw new ServerConfigurationException("server.identity.certificate-pem must be configured before forwarding authentication requests");
        }
    }
}
