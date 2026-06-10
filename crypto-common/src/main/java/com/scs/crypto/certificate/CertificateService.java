package com.scs.crypto.certificate;

import com.scs.crypto.config.CryptoConstants;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class CertificateService {

    // BC must be registered before JcaContentSignerBuilder.build() or JcaX509CertificateConverter are
    // called with provider="BC". Static final String constants are inlined by javac, so CryptoConstants'
    // static block is never triggered via field accesses — register here at class-load time instead.
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * Issues an X.509 v3 certificate for {@code subjectPublicKey} signed by the TTP CA private key.
     * The issuer DN is fixed to {@link CryptoConstants#TTP_ISSUER_DN}; the serial number is derived
     * from the current epoch millisecond to avoid collisions in the single-CA in-memory model.
     *
     * @throws OperatorCreationException if the signing algorithm is unavailable
     * @throws CertificateException      if the certificate cannot be converted to the JCA type
     */
    public X509Certificate generateCertificate(
            PublicKey subjectPublicKey,
            PrivateKey caPrivateKey,
            String subjectDN,
            int validityDays
    ) throws OperatorCreationException, CertificateException {
        X500Name issuer = new X500Name(CryptoConstants.TTP_ISSUER_DN);
        X500Name subject = new X500Name(subjectDN);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = Date.from(Instant.now());
        Date notAfter = Date.from(Instant.now().plus(validityDays, ChronoUnit.DAYS));

        JcaX509v3CertificateBuilder certBuilder =
                new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, subjectPublicKey);

        ContentSigner signer = new JcaContentSignerBuilder(CryptoConstants.SIGNATURE_ALGORITHM)
                .setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER)
                .build(caPrivateKey);

        X509CertificateHolder holder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER)
                .getCertificate(holder);
    }

    /**
     * Encodes an {@link X509Certificate} to PEM ({@code -----BEGIN CERTIFICATE-----}).
     */
    public String encodeCertificatePem(X509Certificate certificate)
            throws CertificateEncodingException, IOException {
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
        }
        return sw.toString();
    }

    /**
     * Decodes a PEM certificate ({@code -----BEGIN CERTIFICATE-----}) into an {@link X509Certificate}.
     *
     * @throws CertificateException if the PEM does not contain a valid X.509 certificate
     */
    public X509Certificate decodeCertificatePem(String pem) throws IOException, CertificateException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter()
                        .setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER)
                        .getCertificate(holder);
            }
            throw new CertificateException("PEM does not contain an X.509 certificate");
        }
    }

    /**
     * Verifies the certificate's signature against the given CA trust anchor.
     * Returns {@code false} for any verification failure including expired or revoked certificates.
     */
    public boolean validateCertificate(X509Certificate certificate, PublicKey caTrustAnchor) {
        try {
            certificate.verify(caTrustAnchor);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the certificate is currently outside its validity window.
     */
    public boolean isCertificateExpired(X509Certificate certificate) {
        try {
            certificate.checkValidity();
            return false;
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            return true;
        }
    }

    public PublicKey extractPublicKeyFromCertificate(X509Certificate certificate) {
        return certificate.getPublicKey();
    }

    public String extractSubjectDN(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().getName();
    }
}
