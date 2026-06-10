package com.scs.ttp.service;

import com.scs.crypto.certificate.CertificateService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.hash.HashService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.ttp.exception.DuplicateIdentityException;
import com.scs.ttp.exception.TtpOperationException;
import com.scs.ttp.model.IdentityType;
import com.scs.ttp.model.RegisteredIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.ldap.Rdn;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RsaKeyService rsaKeyService;
    private final RsaEncryptionService rsaEncryptionService;
    private final EncodingService encodingService;
    private final HashService hashService;
    private final CertificateService certificateService;
    private final TtpCertificateAuthority certificateAuthority;
    private final InMemoryIdentityStore identityStore;

    public RegistrationResponse registerUser(RegistrationRequest request) {
        return register(request, IdentityType.USER);
    }

    public RegistrationResponse registerServer(RegistrationRequest request) {
        return register(request, IdentityType.SERVER);
    }

    public RegistrationResponse getCertificate(String identityId) {
        RegisteredIdentity identity = identityStore.getIdentity(identityId);
        return toResponse(identity);
    }

    private RegistrationResponse register(RegistrationRequest request, IdentityType type) {
        try {
            log.info("Received {} registration attempt for identityName={}", type, request.getIdentityName());
            if (identityStore.identityExists(request.getIdentityName())) {
                throw new DuplicateIdentityException(request.getIdentityName());
            }
            String identityId = decryptAndValidateIdentityId(request);
            if (identityStore.identityIdExists(identityId)) {
                throw new DuplicateIdentityException(identityId);
            }
            PublicKey publicKey = rsaKeyService.decodePublicKeyPem(request.getPublicKeyPem());
            String subjectDN = subjectDn(request.getIdentityName(), type);
            X509Certificate certificate = certificateAuthority.signCertificate(publicKey, subjectDN);
            RegisteredIdentity identity = identityStore.registerIdentity(
                    request.getIdentityName(),
                    identityId,
                    publicKey,
                    type,
                    certificate
            );
            return toResponse(identity);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new TtpOperationException("Failed to register identity", e);
        }
    }

    private String decryptAndValidateIdentityId(RegistrationRequest request) {
        try {
            byte[] encryptedIdentityId = encodingService.decodeBase64(request.getEncryptedIdentityId());
            String identityId = new String(
                    rsaEncryptionService.decrypt(encryptedIdentityId, certificateAuthority.getTtpKeyPair().getPrivate()),
                    java.nio.charset.StandardCharsets.UTF_8
            );
            if (identityId.isBlank()) {
                throw new TtpOperationException("Decrypted identity ID is blank", null);
            }
            String expected = hashService.hashIdentity(identityStore.normalizeName(request.getIdentityName()));
            if (!expected.equals(identityId)) {
                throw new TtpOperationException("Encrypted identity ID does not match identity name hash", null);
            }
            return identityId;
        } catch (TtpOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new TtpOperationException("Failed to decrypt encrypted identity ID", e);
        }
    }

    private RegistrationResponse toResponse(RegisteredIdentity identity) {
        try {
            return RegistrationResponse.builder()
                    .identityId(identity.getIdentityId())
                    .certificatePem(certificateService.encodeCertificatePem(identity.getCertificate()))
                    .registeredAt(identity.getRegisteredAt().toString())
                    .build();
        } catch (Exception e) {
            throw new TtpOperationException("Failed to encode certificate", e);
        }
    }

    private String subjectDn(String identityName, IdentityType type) {
        return "CN=" + Rdn.escapeValue(identityName.trim()) + ",OU=" + type + ",O=SCS,C=PL";
    }
}
