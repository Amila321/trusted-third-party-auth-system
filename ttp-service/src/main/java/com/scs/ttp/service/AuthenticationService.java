package com.scs.ttp.service;

import com.scs.crypto.certificate.CertificateService;
import com.scs.crypto.config.CryptoConstants;
import com.scs.crypto.encoding.EncodingService;
import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.ttp.exception.IdentityNotFoundException;
import com.scs.ttp.exception.TtpOperationException;
import com.scs.ttp.model.AuthenticationSession;
import com.scs.ttp.model.IdentityType;
import com.scs.ttp.model.RegisteredIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CertificateService certificateService;
    private final EncodingService encodingService;
    private final TtpCertificateAuthority certificateAuthority;
    private final InMemoryIdentityStore identityStore;
    private final SessionKeyService sessionKeyService;
    private final InMemorySessionStore sessionStore;

    public TtpAuthenticationDecision authenticateUserForServer(ServerAuthenticationRequest request) {
        try {
            RegisteredIdentity user = identityStore.getIdentity(request.getUserId());
            RegisteredIdentity server = identityStore.getIdentity(request.getServerId());

            X509Certificate presentedUserCertificate = certificateService.decodeCertificatePem(request.getUserCertificatePem());
            X509Certificate presentedServerCertificate = certificateService.decodeCertificatePem(request.getServerCertificatePem());

            String rejection = validateRegisteredIdentity(user, IdentityType.USER, presentedUserCertificate);
            if (rejection != null) {
                return rejected(rejection);
            }

            rejection = validateRegisteredIdentity(server, IdentityType.SERVER, presentedServerCertificate);
            if (rejection != null) {
                return rejected(rejection);
            }

            if (!verifyChallengeSignature(request.getChallenge(), request.getSignedChallenge(), presentedUserCertificate.getPublicKey())) {
                return rejected("Invalid signed challenge");
            }

            SecretKey sessionKey = sessionKeyService.generateSessionKey();
            AuthenticationSession session = sessionStore.createSession(request.getUserId(), request.getServerId(), sessionKey);
            String encryptedForUser = sessionKeyService.encryptSessionKeyForRecipient(sessionKey, presentedUserCertificate.getPublicKey());
            String encryptedForServer = sessionKeyService.encryptSessionKeyForRecipient(sessionKey, presentedServerCertificate.getPublicKey());

            log.info("Authentication accepted sessionId={} userId={} serverId={}",
                    session.getSessionId(),
                    request.getUserId(),
                    request.getServerId());
            return TtpAuthenticationDecision.builder()
                    .authenticated(true)
                    .sessionId(session.getSessionId())
                    .encryptedSessionKeyForUser(encryptedForUser)
                    .encryptedSessionKeyForServer(encryptedForServer)
                    .decidedAt(Instant.now().toString())
                    .build();
        } catch (IdentityNotFoundException e) {
            log.warn("Authentication rejected: {}", e.getMessage());
            return rejected(e.getMessage());
        } catch (Exception e) {
            throw new TtpOperationException("Failed to authenticate user for server", e);
        }
    }

    private String validateRegisteredIdentity(
            RegisteredIdentity registeredIdentity,
            IdentityType expectedType,
            X509Certificate presentedCertificate
    ) throws Exception {
        if (registeredIdentity.getType() != expectedType) {
            return "Registered identity type mismatch";
        }
        if (certificateService.isCertificateExpired(presentedCertificate)) {
            return "Certificate is expired or not yet valid";
        }
        if (!certificateService.validateCertificate(presentedCertificate, certificateAuthority.getTtpKeyPair().getPublic())) {
            return "Certificate was not signed by the TTP";
        }
        if (!Arrays.equals(registeredIdentity.getCertificate().getEncoded(), presentedCertificate.getEncoded())) {
            return "Presented certificate does not match registered identity";
        }
        return null;
    }

    private boolean verifyChallengeSignature(String challenge, String signedChallenge, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(CryptoConstants.SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes(StandardCharsets.UTF_8));
            return signature.verify(encodingService.decodeBase64(signedChallenge));
        } catch (Exception e) {
            log.warn("Failed to verify signed challenge", e);
            return false;
        }
    }

    private TtpAuthenticationDecision rejected(String reason) {
        log.info("Authentication rejected reason={}", reason);
        return TtpAuthenticationDecision.builder()
                .authenticated(false)
                .rejectionReason(reason)
                .decidedAt(Instant.now().toString())
                .build();
    }
}
