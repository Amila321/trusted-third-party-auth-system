package com.scs.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.crypto.aes.AesEncryptionService;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.server.dto.SessionCloseRequest;
import com.scs.server.model.ServerSessionContext;
import com.scs.server.service.InMemoryServerSessionStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AesKeyService aesKeyService;

    @Autowired
    private AesEncryptionService aesEncryptionService;

    @Autowired
    private EncodingService encodingService;

    @Autowired
    private InMemoryServerSessionStore sessionStore;

    @Test
    void decryptAndProcessReturnsEncryptedServerResponse() throws Exception {
        SecretKey sessionKey = aesKeyService.generateSessionKey();
        String sessionId = seedSession(sessionKey);
        EncryptedDataRequest request = encryptedRequest(sessionId, "hello secure server", sessionKey);

        String responseBody = mockMvc.perform(post("/api/server/data/decrypt-and-process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value(sessionId))
                .andExpect(jsonPath("$.ciphertext").isNotEmpty())
                .andExpect(jsonPath("$.iv").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        byte[] plaintext = aesEncryptionService.decrypt(
                encodingService.decodeBase64(response.get("ciphertext").asText()),
                sessionKey,
                encodingService.decodeBase64(response.get("iv").asText())
        );

        assertThat(new String(plaintext, StandardCharsets.UTF_8)).isEqualTo("server processed: hello secure server");
    }

    @Test
    void decryptAndProcessRejectsMissingSession() throws Exception {
        SecretKey sessionKey = aesKeyService.generateSessionKey();

        mockMvc.perform(post("/api/server/data/decrypt-and-process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(encryptedRequest("missing-session", "payload", sessionKey))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("DATA_EXCHANGE_FAILED"));
    }

    @Test
    void decryptAndProcessRejectsInvalidCiphertext() throws Exception {
        String sessionId = seedSession(aesKeyService.generateSessionKey());

        mockMvc.perform(post("/api/server/data/decrypt-and-process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(EncryptedDataRequest.builder()
                                .sessionId(sessionId)
                                .ciphertext("not-base64")
                                .iv("not-base64")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("DATA_EXCHANGE_FAILED"));
    }

    @Test
    void decryptAndProcessRejectsSessionWithoutDecryptedKey() throws Exception {
        String sessionId = "session-no-key-" + UUID.randomUUID();
        sessionStore.save(ServerSessionContext.builder()
                .sessionId(sessionId)
                .userId("user")
                .serverId("server")
                .encryptedSessionKeyForServer("encrypted-only")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(post("/api/server/data/decrypt-and-process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(EncryptedDataRequest.builder()
                                .sessionId(sessionId)
                                .ciphertext("ciphertext")
                                .iv("iv")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("DATA_EXCHANGE_FAILED"));
    }

    @Test
    void closeSessionRemovesSessionContext() throws Exception {
        String sessionId = seedSession(aesKeyService.generateSessionKey());

        mockMvc.perform(post("/api/server/session/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SessionCloseRequest.builder()
                                .sessionId(sessionId)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value(sessionId))
                .andExpect(jsonPath("$.closed").value(true));

        assertThat(sessionStore.findBySessionId(sessionId)).isEmpty();
    }

    private String seedSession(SecretKey sessionKey) {
        String sessionId = "session-" + UUID.randomUUID();
        sessionStore.save(ServerSessionContext.builder()
                .sessionId(sessionId)
                .userId("user-1")
                .serverId("server-1")
                .sessionKey(sessionKey)
                .createdAt(Instant.now())
                .build());
        return sessionId;
    }

    private EncryptedDataRequest encryptedRequest(String sessionId, String plaintext, SecretKey sessionKey) throws Exception {
        byte[] iv = aesEncryptionService.generateIv();
        byte[] ciphertext = aesEncryptionService.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), sessionKey, iv);
        return EncryptedDataRequest.builder()
                .sessionId(sessionId)
                .ciphertext(encodingService.encodeBase64(ciphertext))
                .iv(encodingService.encodeBase64(iv))
                .build();
    }
}
