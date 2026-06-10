package com.scs.crypto.hash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashServiceTest {

    private HashService service;

    @BeforeEach
    void setUp() {
        service = new HashService();
    }

    @Test
    void hashIdentity_sameInput_producesConsistentOutput() throws Exception {
        String h1 = service.hashIdentity("alice");
        String h2 = service.hashIdentity("alice");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void hashIdentity_differentInputs_produceDifferentHashes() throws Exception {
        assertThat(service.hashIdentity("alice")).isNotEqualTo(service.hashIdentity("bob"));
    }

    @Test
    void hashIdentity_producesHexString() throws Exception {
        String hash = service.hashIdentity("alice");
        assertThat(hash).matches("[0-9a-f]{64}"); // SHA-256 = 32 bytes = 64 hex chars
    }

    @Test
    void hashChallenge_sameInputs_producesConsistentOutput() throws Exception {
        String h1 = service.hashChallenge("challenge", "salt");
        String h2 = service.hashChallenge("challenge", "salt");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void hashChallenge_differentSalt_producesDifferentHash() throws Exception {
        String h1 = service.hashChallenge("challenge", "salt1");
        String h2 = service.hashChallenge("challenge", "salt2");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void hashChallenge_differentChallenge_producesDifferentHash() throws Exception {
        String h1 = service.hashChallenge("challenge1", "salt");
        String h2 = service.hashChallenge("challenge2", "salt");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void hashChallenge_producesHexString() throws Exception {
        String hash = service.hashChallenge("challenge", "salt");
        assertThat(hash).matches("[0-9a-f]{64}");
    }
}
