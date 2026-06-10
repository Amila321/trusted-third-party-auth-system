package com.scs.crypto.aes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

class AesKeyServiceTest {

    private AesKeyService service;

    @BeforeEach
    void setUp() {
        service = new AesKeyService();
    }

    @Test
    void generateSessionKey_producesNonNullKey() throws Exception {
        assertThat(service.generateSessionKey()).isNotNull();
    }

    @Test
    void generateSessionKey_algorithmIsAes() throws Exception {
        assertThat(service.generateSessionKey().getAlgorithm()).isEqualTo("AES");
    }

    @Test
    void generateSessionKey_keyLengthIs256Bits() throws Exception {
        SecretKey key = service.generateSessionKey();
        assertThat(key.getEncoded()).hasSize(32); // 256 bits = 32 bytes
    }

    @Test
    void encodeKey_decodeKey_roundTrip() throws Exception {
        SecretKey original = service.generateSessionKey();
        String encoded = service.encodeKey(original);
        SecretKey decoded = service.decodeKey(encoded);
        assertThat(decoded.getEncoded()).isEqualTo(original.getEncoded());
        assertThat(decoded.getAlgorithm()).isEqualTo("AES");
    }

    @Test
    void generateSessionKey_twoKeys_areDistinct() throws Exception {
        SecretKey k1 = service.generateSessionKey();
        SecretKey k2 = service.generateSessionKey();
        assertThat(k1.getEncoded()).isNotEqualTo(k2.getEncoded());
    }
}
