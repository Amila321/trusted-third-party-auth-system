package com.scs.crypto.rsa;

import com.scs.crypto.config.CryptoConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;

import static org.assertj.core.api.Assertions.assertThat;

class RsaKeyServiceTest {

    private static RsaKeyService service;
    private static KeyPair keyPair;

    // RSA-4096 key generation is slow; generate once for the whole class.
    @BeforeAll
    static void generateKeys() throws Exception {
        service = new RsaKeyService();
        keyPair = service.generateKeyPair();
    }

    @Test
    void generateKeyPair_returnsNonNullPair() {
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isNotNull();
        assertThat(keyPair.getPrivate()).isNotNull();
    }

    @Test
    void generateKeyPair_algorithmIsRsa() {
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void generateKeyPair_keyLengthIs4096Bits() {
        RSAKey rsaPublicKey = (RSAKey) keyPair.getPublic();
        assertThat(rsaPublicKey.getModulus().bitLength()).isEqualTo(CryptoConstants.RSA_KEY_SIZE);
    }

    @Test
    void encodePublicKeyPem_containsHeader() throws Exception {
        String pem = service.encodePublicKeyPem(keyPair.getPublic());
        assertThat(pem).contains("-----BEGIN PUBLIC KEY-----");
        assertThat(pem).contains("-----END PUBLIC KEY-----");
    }

    @Test
    void encodeDecodePublicKey_roundTrip() throws Exception {
        String pem = service.encodePublicKeyPem(keyPair.getPublic());
        PublicKey decoded = service.decodePublicKeyPem(pem);
        assertThat(decoded.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    }

    @Test
    void encodePrivateKeyPem_containsHeader() throws Exception {
        String pem = service.encodePrivateKeyPem(keyPair.getPrivate());
        assertThat(pem).contains("-----BEGIN PRIVATE KEY-----");
        assertThat(pem).contains("-----END PRIVATE KEY-----");
    }

    @Test
    void encodeDecodePrivateKey_roundTrip() throws Exception {
        String pem = service.encodePrivateKeyPem(keyPair.getPrivate());
        PrivateKey decoded = service.decodePrivateKeyPem(pem);
        assertThat(decoded.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
    }
}
