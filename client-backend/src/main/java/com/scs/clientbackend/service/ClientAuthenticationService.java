package com.scs.clientbackend.service;

import com.scs.clientbackend.dto.ClientAuthenticationInitiateRequest;
import com.scs.clientbackend.dto.ClientAuthenticationStateResponse;
import com.scs.clientbackend.dto.ClientSessionStatusResponse;
import com.scs.clientbackend.exception.ClientAuthenticationException;
import com.scs.clientbackend.exception.ServerConnectionException;
import com.scs.clientbackend.exception.TtpClientException;
import com.scs.clientbackend.model.ClientIdentityContext;
import com.scs.clientbackend.model.ClientSessionContext;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.config.CryptoConstants;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.hash.HashService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.IdentityStatusResponse;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.TtpPublicKeyResponse;
import com.scs.dto.auth.UserAuthenticationRequest;
import com.scs.dto.session.SessionKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAuthenticationService {

    private final RsaKeyService rsaKeyService;
    private final RsaEncryptionService rsaEncryptionService;
    private final AesKeyService aesKeyService;
    private final EncodingService encodingService;
    private final HashService hashService;
    private final TtpClient ttpClient;
    private final ServerConnection serverConnection;
    private final InMemoryClientAuthenticationStore authenticationStore;

    public ClientAuthenticationStateResponse initiate(ClientAuthenticationInitiateRequest request) {
        try {
            KeyPair keyPair = rsaKeyService.generateKeyPair();
            String publicKeyPem = rsaKeyService.encodePublicKeyPem(keyPair.getPublic());
            TtpPublicKeyResponse ttpPublicKey = ttpClient.getTtpPublicKey();
            PublicKey ttpKey = rsaKeyService.decodePublicKeyPem(ttpPublicKey.getPublicKeyPem());
            String identityId = hashService.hashIdentity(request.getIdentityName().trim().toLowerCase(Locale.ROOT));
            String encryptedIdentityId = encodingService.encodeBase64(
                    rsaEncryptionService.encrypt(identityId.getBytes(StandardCharsets.UTF_8), ttpKey)
            );
            RegistrationResponse registrationResponse = ttpClient.registerUser(RegistrationRequest.builder()
                    .identityName(request.getIdentityName())
                    .encryptedIdentityId(encryptedIdentityId)
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

    public TtpAuthenticationDecision requestSession() {
        ClientIdentityContext user = authenticationStore.findActiveIdentity()
                .orElseThrow(() -> new ClientAuthenticationException("User identity is not registered at TTP yet."));
        IdentityStatusResponse server = serverConnection.getServerIdentity();
        if (!server.isRegistered()) {
            throw new ClientAuthenticationException("Server identity is not registered at TTP yet.");
        }

        try {
            String challenge = newChallenge();
            String signedChallenge = signChallenge(challenge, user.getKeyPair());
            UserAuthenticationRequest request = UserAuthenticationRequest.builder()
                    .userId(user.getIdentityId())
                    .serverId(server.getIdentityId())
                    .userCertificatePem(user.getCertificatePem())
                    .challenge(challenge)
                    .signedChallenge(signedChallenge)
                    .build();
            return serverConnection.requestSession(request);
        } catch (ClientAuthenticationException e) {
            throw e;
        } catch (ServerConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientAuthenticationException("Failed to request session through server", e);
        }
    }

    public ClientSessionStatusResponse requestAndCompleteSession() {
        TtpAuthenticationDecision decision = requestSession();
        if (!decision.isAuthenticated()) {
            throw new ClientAuthenticationException("TTP rejected authentication: " + decision.getRejectionReason());
        }
        return complete(SessionKeyResponse.builder()
                .sessionId(decision.getSessionId())
                .encryptedSessionKey(decision.getEncryptedSessionKeyForUser())
                .issuedAt(decision.getDecidedAt())
                .build());
    }

    private String newChallenge() {
        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);
        return Base64.getEncoder().encodeToString(challenge);
    }

    private String signChallenge(String challenge, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance(CryptoConstants.SIGNATURE_ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(challenge.getBytes(StandardCharsets.UTF_8));
        return encodingService.encodeBase64(signature.sign());
    }
}
