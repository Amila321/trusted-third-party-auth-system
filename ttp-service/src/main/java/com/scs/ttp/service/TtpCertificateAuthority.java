package com.scs.ttp.service;

import com.scs.crypto.certificate.CertificateService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.ttp.exception.TtpOperationException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtpCertificateAuthority {

    private final RsaKeyService rsaKeyService;
    private final CertificateService certificateService;

    @Value("${ttp.certificate.validity-days:365}")
    private int certificateValidityDays;

    @Getter
    private KeyPair ttpKeyPair;

    @PostConstruct
    void initializeKeyPair() {
        try {
            this.ttpKeyPair = rsaKeyService.generateKeyPair();
            log.info("Generated in-memory TTP CA RSA key pair");
        } catch (Exception e) {
            throw new TtpOperationException("Failed to initialize TTP CA key pair", e);
        }
    }

    public X509Certificate signCertificate(PublicKey subjectKey, String subjectDN) {
        try {
            X509Certificate certificate = certificateService.generateCertificate(
                    subjectKey,
                    ttpKeyPair.getPrivate(),
                    subjectDN,
                    certificateValidityDays
            );
            log.info("Issued certificate for subjectDN={}", subjectDN);
            return certificate;
        } catch (Exception e) {
            throw new TtpOperationException("Failed to sign certificate for " + subjectDN, e);
        }
    }
}
