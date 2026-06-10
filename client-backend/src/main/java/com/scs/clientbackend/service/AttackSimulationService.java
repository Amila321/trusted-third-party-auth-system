package com.scs.clientbackend.service;

import com.scs.clientbackend.exception.ClientAuthenticationException;
import com.scs.clientbackend.model.ClientIdentityContext;
import com.scs.crypto.certificate.CertificateService;
import com.scs.crypto.config.CryptoConstants;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.dto.attack.AttackScenarioType;
import com.scs.dto.attack.AttackSimulationRequest;
import com.scs.dto.attack.AttackSimulationResponse;
import com.scs.dto.auth.IdentityStatusResponse;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.naming.ldap.Rdn;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttackSimulationService {

    private final InMemoryClientAuthenticationStore authenticationStore;
    private final ServerConnection serverConnection;
    private final RsaKeyService rsaKeyService;
    private final CertificateService certificateService;
    private final EncodingService encodingService;

    public AttackSimulationResponse simulate(AttackSimulationRequest request) {
        ClientIdentityContext user = authenticationStore.findActiveIdentity()
                .orElseThrow(() -> new ClientAuthenticationException("User identity is not registered at TTP yet."));
        IdentityStatusResponse server = serverConnection.getServerIdentity();
        if (!server.isRegistered()) {
            throw new ClientAuthenticationException("Server identity is not registered at TTP yet.");
        }

        AttackMaterial material = attackMaterial(request.getScenario(), user);
        TtpAuthenticationDecision decision = serverConnection.requestSession(UserAuthenticationRequest.builder()
                .userId(user.getIdentityId())
                .serverId(server.getIdentityId())
                .userCertificatePem(material.userCertificatePem())
                .challenge(material.challenge())
                .signedChallenge(material.signedChallenge())
                .build());

        return AttackSimulationResponse.builder()
                .scenario(request.getScenario())
                .attackName(attackName(request.getScenario()))
                .description(description(request.getScenario()))
                .expectedRejection(true)
                .rejected(!decision.isAuthenticated())
                .ttpDecision(decision)
                .evidence(evidence(request.getScenario(), user, server, material, decision))
                .build();
    }

    private AttackMaterial attackMaterial(AttackScenarioType scenario, ClientIdentityContext user) {
        return switch (scenario) {
            case FORGED_USER_CERTIFICATE -> forgedCertificateMaterial(user, user.getIdentityName());
            case TAMPERED_USER_CERTIFICATE -> forgedCertificateMaterial(user, user.getIdentityName() + "-tampered");
            case INVALID_CHALLENGE_SIGNATURE -> invalidSignatureMaterial(user);
            case IDENTITY_CERTIFICATE_MISMATCH -> forgedCertificateMaterial(user, "different-identity-" + UUID.randomUUID());
        };
    }

    private AttackMaterial forgedCertificateMaterial(ClientIdentityContext user, String subjectName) {
        try {
            KeyPair attackerKeyPair = rsaKeyService.generateKeyPair();
            X509Certificate forgedCertificate = certificateService.generateSelfSignedCertificate(
                    attackerKeyPair,
                    "CN=" + Rdn.escapeValue(subjectName) + ",OU=USER,O=Attacker,C=PL",
                    CryptoConstants.CERTIFICATE_VALIDITY_DAYS
            );
            String challenge = randomChallenge();
            return new AttackMaterial(
                    certificateService.encodeCertificatePem(forgedCertificate),
                    challenge,
                    sign(challenge, attackerKeyPair),
                    true,
                    false,
                    true
            );
        } catch (Exception e) {
            throw new ClientAuthenticationException("Failed to generate forged certificate attack material", e);
        }
    }

    private AttackMaterial invalidSignatureMaterial(ClientIdentityContext user) {
        try {
            KeyPair attackerKeyPair = rsaKeyService.generateKeyPair();
            String challenge = randomChallenge();
            return new AttackMaterial(
                    user.getCertificatePem(),
                    challenge,
                    sign(challenge, attackerKeyPair),
                    false,
                    true,
                    true
            );
        } catch (Exception e) {
            throw new ClientAuthenticationException("Failed to generate invalid signature attack material", e);
        }
    }

    private String sign(String challenge, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance(CryptoConstants.SIGNATURE_ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(challenge.getBytes(StandardCharsets.UTF_8));
        return encodingService.encodeBase64(signature.sign());
    }

    private String randomChallenge() {
        return UUID.randomUUID().toString();
    }

    private Map<String, Object> evidence(
            AttackScenarioType scenario,
            ClientIdentityContext user,
            IdentityStatusResponse server,
            AttackMaterial material,
            TtpAuthenticationDecision decision
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("used_real_user_id", true);
        evidence.put("user_id", user.getIdentityId());
        evidence.put("server_id", server.getIdentityId());
        evidence.put("used_forged_certificate", material.usedForgedCertificate());
        evidence.put("used_real_user_certificate", material.usedRealUserCertificate());
        evidence.put("used_real_server_certificate", true);
        evidence.put("used_invalid_signature", material.usedInvalidSignature());
        evidence.put("session_created", decision.isAuthenticated());
        evidence.put("scenario_executed", scenario.name());
        return evidence;
    }

    private String attackName(AttackScenarioType scenario) {
        return switch (scenario) {
            case FORGED_USER_CERTIFICATE -> "Forged User Certificate Attack";
            case TAMPERED_USER_CERTIFICATE -> "Tampered User Certificate Attack";
            case INVALID_CHALLENGE_SIGNATURE -> "Invalid Challenge Signature Attack";
            case IDENTITY_CERTIFICATE_MISMATCH -> "Identity/Certificate Mismatch Attack";
        };
    }

    private String description(AttackScenarioType scenario) {
        return switch (scenario) {
            case FORGED_USER_CERTIFICATE -> "A rogue certificate is presented for the real registered user id.";
            case TAMPERED_USER_CERTIFICATE -> "A replacement certificate is presented instead of the registered user certificate.";
            case INVALID_CHALLENGE_SIGNATURE -> "The real certificate is used, but the challenge is signed by a rogue private key.";
            case IDENTITY_CERTIFICATE_MISMATCH -> "The real user id is paired with a certificate for a different identity.";
        };
    }

    private record AttackMaterial(
            String userCertificatePem,
            String challenge,
            String signedChallenge,
            boolean usedForgedCertificate,
            boolean usedRealUserCertificate,
            boolean usedInvalidSignature
    ) {
    }
}
