package com.scs.server.service;

import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.server.exception.ServerConfigurationException;
import com.scs.server.exception.SessionDecisionException;
import com.scs.server.model.ServerIdentityContext;
import com.scs.server.model.ServerSessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final InMemoryServerIdentityStore identityStore;
    private final RsaEncryptionService rsaEncryptionService;
    private final AesKeyService aesKeyService;
    private final EncodingService encodingService;

    public TtpAuthenticationDecision handleUserServiceRequest(UserAuthenticationRequest request) {
        ServerIdentityContext serverIdentity = identityStore.current()
                .orElseThrow(() -> new ServerConfigurationException("Server identity is not registered at TTP yet."));
        ServerAuthenticationRequest ttpRequest = ServerAuthenticationRequest.builder()
                .userId(request.getUserId())
                .serverId(serverIdentity.getIdentityId())
                .userCertificatePem(request.getUserCertificatePem())
                .serverCertificatePem(serverIdentity.getCertificatePem())
                .challenge(request.getChallenge())
                .signedChallenge(request.getSignedChallenge())
                .build();

        TtpAuthenticationDecision decision = ttpClient.authenticateUserForServer(ttpRequest);
        log.info("Received TTP authentication decision userId={} serverId={} authenticated={}",
                request.getUserId(),
                request.getServerId(),
                decision.isAuthenticated());

        if (decision.isAuthenticated()) {
            storeAuthenticatedSession(request, decision, serverIdentity);
        }
        return decision;
    }

    private void storeAuthenticatedSession(
            UserAuthenticationRequest request,
            TtpAuthenticationDecision decision,
            ServerIdentityContext serverIdentity
    ) {
        if (!StringUtils.hasText(decision.getSessionId())) {
            throw new SessionDecisionException("Authenticated TTP decision did not include a session ID");
        }
        if (!StringUtils.hasText(decision.getEncryptedSessionKeyForServer())) {
            throw new SessionDecisionException("Authenticated TTP decision did not include the server session key");
        }

        sessionStore.save(ServerSessionContext.builder()
                .sessionId(decision.getSessionId())
                .userId(request.getUserId())
                .serverId(serverIdentity.getIdentityId())
                .encryptedSessionKeyForServer(decision.getEncryptedSessionKeyForServer())
                .sessionKey(decryptSessionKeyForServer(decision, serverIdentity))
                .createdAt(Instant.now())
                .build());
    }

    private SecretKey decryptSessionKeyForServer(TtpAuthenticationDecision decision, ServerIdentityContext serverIdentity) {
        try {
            byte[] encryptedKey = encodingService.decodeBase64(decision.getEncryptedSessionKeyForServer());
            byte[] keyBytes = rsaEncryptionService.decrypt(encryptedKey, serverIdentity.getKeyPair().getPrivate());
            return aesKeyService.decodeKey(encodingService.encodeBase64(keyBytes));
        } catch (Exception e) {
            throw new SessionDecisionException("Failed to decrypt server session key from TTP decision");
        }
    }
}
