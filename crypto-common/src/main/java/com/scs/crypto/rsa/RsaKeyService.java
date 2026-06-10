package com.scs.crypto.rsa;

import com.scs.crypto.config.CryptoConstants;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;

public class RsaKeyService {

    /**
     * Generates a new RSA-4096 key pair using a cryptographically strong random source.
     *
     * @throws NoSuchAlgorithmException if RSA is not available (should never occur on standard JVMs)
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(CryptoConstants.RSA_ALGORITHM);
        generator.initialize(CryptoConstants.RSA_KEY_SIZE, new SecureRandom());
        return generator.generateKeyPair();
    }

    /**
     * Encodes a public key to PEM (X.509 SubjectPublicKeyInfo, {@code BEGIN PUBLIC KEY}).
     */
    public String encodePublicKeyPem(PublicKey publicKey) throws IOException {
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        }
        return sw.toString();
    }

    /**
     * Decodes a PEM public key ({@code BEGIN PUBLIC KEY}) into a {@link PublicKey}.
     *
     * @throws InvalidKeyException if the PEM does not contain a recognized public key structure
     */
    public PublicKey decodePublicKeyPem(String pem) throws IOException, GeneralSecurityException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj instanceof SubjectPublicKeyInfo info) {
                return new JcaPEMKeyConverter().getPublicKey(info);
            }
            throw new InvalidKeyException("PEM does not contain a public key");
        }
    }

    /**
     * Encodes a private key to PEM (PKCS#8, {@code BEGIN PRIVATE KEY}).
     */
    public String encodePrivateKeyPem(PrivateKey privateKey) throws IOException {
        StringWriter sw = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(sw)) {
            pemWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
        }
        return sw.toString();
    }

    /**
     * Decodes a PEM private key (PKCS#8 or PKCS#1 RSA format) into a {@link PrivateKey}.
     *
     * @throws InvalidKeyException if the PEM does not contain a recognized private key structure
     */
    public PrivateKey decodePrivateKeyPem(String pem) throws IOException, GeneralSecurityException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (obj instanceof PrivateKeyInfo info) {
                return converter.getPrivateKey(info);
            }
            if (obj instanceof PEMKeyPair keyPair) {
                return converter.getKeyPair(keyPair).getPrivate();
            }
            throw new InvalidKeyException("PEM does not contain a private key");
        }
    }
}
