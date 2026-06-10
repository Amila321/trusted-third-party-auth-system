package com.scs.crypto.aes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

class AesEncryptionServiceTest {

    private AesEncryptionService service;
    private AesKeyService keyService;

    @BeforeEach
    void setUp() {
        service = new AesEncryptionService();
        keyService = new AesKeyService();
    }

    @Test
    void encrypt_decrypt_roundTrip() throws Exception {
        SecretKey key = keyService.generateSessionKey();
        byte[] iv = service.generateIv();
        byte[] plaintext = "sensitive payload".getBytes();

        byte[] ciphertext = service.encrypt(plaintext, key, iv);
        byte[] decrypted = service.decrypt(ciphertext, key, iv);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void generateIv_produces16Bytes() {
        assertThat(service.generateIv()).hasSize(16);
    }

    @Test
    void generateIv_twoCalls_produceDistinctValues() {
        byte[] iv1 = service.generateIv();
        byte[] iv2 = service.generateIv();
        assertThat(iv1).isNotEqualTo(iv2);
    }

    @Test
    void encrypt_differentIv_producesDifferentCiphertext() throws Exception {
        SecretKey key = keyService.generateSessionKey();
        byte[] plaintext = "same plaintext for both".getBytes();

        byte[] ct1 = service.encrypt(plaintext, key, service.generateIv());
        byte[] ct2 = service.encrypt(plaintext, key, service.generateIv());

        assertThat(ct1).isNotEqualTo(ct2);
    }

    @Test
    void encrypt_ciphertextDiffersFromPlaintext() throws Exception {
        SecretKey key = keyService.generateSessionKey();
        byte[] iv = service.generateIv();
        byte[] plaintext = "hello world 1234".getBytes(); // 16 bytes, no padding needed

        byte[] ciphertext = service.encrypt(plaintext, key, iv);
        assertThat(ciphertext).isNotEqualTo(plaintext);
    }
}
