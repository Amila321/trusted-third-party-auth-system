package com.scs.crypto.certificate;

import com.scs.crypto.rsa.RsaKeyService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateServiceTest {

    private static CertificateService service;
    private static KeyPair caKeyPair;
    private static KeyPair subjectKeyPair;

    // RSA-4096 key generation is slow; generate both pairs once for the whole class.
    @BeforeAll
    static void setUp() throws Exception {
        RsaKeyService rsaKeyService = new RsaKeyService();
        caKeyPair = rsaKeyService.generateKeyPair();
        subjectKeyPair = rsaKeyService.generateKeyPair();
        service = new CertificateService();
    }

    @Test
    void generateCertificate_returnsNonNullCertificate() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        assertThat(cert).isNotNull();
    }

    @Test
    void generateCertificate_subjectDnIsPreserved() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        assertThat(service.extractSubjectDN(cert)).contains("CN=alice");
    }

    @Test
    void generateCertificate_publicKeyMatchesSubject() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        assertThat(service.extractPublicKeyFromCertificate(cert).getEncoded())
                .isEqualTo(subjectKeyPair.getPublic().getEncoded());
    }

    @Test
    void encodeCertificatePem_containsHeaders() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        String pem = service.encodeCertificatePem(cert);
        assertThat(pem).contains("-----BEGIN CERTIFICATE-----");
        assertThat(pem).contains("-----END CERTIFICATE-----");
    }

    @Test
    void encodeDecode_certificatePem_roundTrip() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        String pem = service.encodeCertificatePem(cert);
        X509Certificate decoded = service.decodeCertificatePem(pem);
        assertThat(decoded.getEncoded()).isEqualTo(cert.getEncoded());
    }

    @Test
    void validateCertificate_signedByCa_returnsTrue() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        assertThat(service.validateCertificate(cert, caKeyPair.getPublic())).isTrue();
    }

    @Test
    void validateCertificate_wrongTrustAnchor_returnsFalse() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        // Validate against subject's own public key, not the CA key → signature verification must fail
        assertThat(service.validateCertificate(cert, subjectKeyPair.getPublic())).isFalse();
    }

    @Test
    void isCertificateExpired_validCert_returnsFalse() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=alice", 365);
        assertThat(service.isCertificateExpired(cert)).isFalse();
    }

    @Test
    void extractPublicKey_returnsCorrectKey() throws Exception {
        X509Certificate cert = service.generateCertificate(
                subjectKeyPair.getPublic(), caKeyPair.getPrivate(), "CN=server", 365);
        assertThat(service.extractPublicKeyFromCertificate(cert).getAlgorithm()).isEqualTo("RSA");
    }
}
