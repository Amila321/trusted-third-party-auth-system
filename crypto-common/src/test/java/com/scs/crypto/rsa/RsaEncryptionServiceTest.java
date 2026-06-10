package com.scs.crypto.rsa;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;

class RsaEncryptionServiceTest {

    private static RsaEncryptionService encryptionService;
    private static KeyPair keyPair;

    // RSA-4096 key generation is slow; generate once for the whole class.
    @BeforeAll
    static void setUp() throws Exception {
        RsaKeyService keyService = new RsaKeyService();
        keyPair = keyService.generateKeyPair();
        encryptionService = new RsaEncryptionService();
    }

    @Test
    void encrypt_decrypt_roundTrip() throws Exception {
        byte[] plaintext = "test message for RSA".getBytes();
        byte[] ciphertext = encryptionService.encrypt(plaintext, keyPair.getPublic());
        byte[] decrypted = encryptionService.decrypt(ciphertext, keyPair.getPrivate());
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_ciphertextDiffersFromPlaintext() throws Exception {
        byte[] plaintext = "hello".getBytes();
        byte[] ciphertext = encryptionService.encrypt(plaintext, keyPair.getPublic());
        assertThat(ciphertext).isNotEqualTo(plaintext);
    }

    @Test
    void encrypt_producesNonDeterministicOutput() throws Exception {
        // PKCS#1 v1.5 padding includes random bytes → same plaintext yields different ciphertext
        byte[] plaintext = "same payload".getBytes();
        byte[] ct1 = encryptionService.encrypt(plaintext, keyPair.getPublic());
        byte[] ct2 = encryptionService.encrypt(plaintext, keyPair.getPublic());
        assertThat(ct1).isNotEqualTo(ct2);
    }

    @Test
    void encrypt_aesKeyPayload_roundTrip() throws Exception {
        // Simulate the typical use-case: RSA-wrapping a 32-byte AES-256 key
        byte[] aesKeyBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(aesKeyBytes);
        byte[] ciphertext = encryptionService.encrypt(aesKeyBytes, keyPair.getPublic());
        byte[] decrypted = encryptionService.decrypt(ciphertext, keyPair.getPrivate());
        assertThat(decrypted).isEqualTo(aesKeyBytes);
    }
}
