package com.scs.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
import com.scs.server.model.ServerIdentityContext;
import com.scs.server.model.ServerSessionContext;
import com.scs.server.service.InMemoryServerIdentityStore;
import com.scs.server.service.InMemoryServerSessionStore;
import com.scs.server.service.TtpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceRequestControllerPrivateKeyTest {

    private static final RsaKeyService RSA_KEY_SERVICE = new RsaKeyService();
    private static final KeyPair SERVER_KEY_PAIR = generateServerKeyPair();
    private static final String SERVER_PRIVATE_KEY_PEM = encodePrivateKey();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AesKeyService aesKeyService;

    @Autowired
    private RsaEncryptionService rsaEncryptionService;

    @Autowired
    private EncodingService encodingService;

    @Autowired
    private InMemoryServerSessionStore sessionStore;

    @Autowired
    private InMemoryServerIdentityStore identityStore;

    @MockBean
    private TtpClient ttpClient;

    @Test
    void acceptedDecisionDecryptsServerSessionKeyWhenPrivateKeyIsConfigured() throws Exception {
        identityStore.save(ServerIdentityContext.builder()
                .identityId("server-1")
                .identityName("server")
                .keyPair(SERVER_KEY_PAIR)
                .publicKeyPem(RSA_KEY_SERVICE.encodePublicKeyPem(SERVER_KEY_PAIR.getPublic()))
                .certificatePem("SERVER_CERTIFICATE_PEM")
                .registeredAt(Instant.now().toString())
                .build());
        SecretKey sessionKey = aesKeyService.generateSessionKey();
        String encryptedForServer = encodingService.encodeBase64(
                rsaEncryptionService.encrypt(sessionKey.getEncoded(), SERVER_KEY_PAIR.getPublic())
        );
        when(ttpClient.authenticateUserForServer(any(ServerAuthenticationRequest.class)))
                .thenReturn(TtpAuthenticationDecision.builder()
                        .authenticated(true)
                        .sessionId("private-key-session")
                        .encryptedSessionKeyForUser("user-key")
                        .encryptedSessionKeyForServer(encryptedForServer)
                        .decidedAt("2026-06-10T12:00:00Z")
                        .build());

        mockMvc.perform(post("/api/server/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UserAuthenticationRequest.builder()
                                .userId("user-1")
                                .serverId("server-1")
                                .userCertificatePem("USER_CERTIFICATE_PEM")
                                .challenge("challenge")
                                .signedChallenge("signed-challenge")
                                .build())))
                .andExpect(status().isOk());

        Optional<ServerSessionContext> stored = sessionStore.findBySessionId("private-key-session");
        assertThat(stored).isPresent();
        assertThat(stored.get().getSessionKey()).isNotNull();
        assertThat(stored.get().getSessionKey().getEncoded()).containsExactly(sessionKey.getEncoded());
    }

    private static KeyPair generateServerKeyPair() {
        try {
            return RSA_KEY_SERVICE.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encodePrivateKey() {
        try {
            return RSA_KEY_SERVICE.encodePrivateKeyPem(SERVER_KEY_PAIR.getPrivate());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
