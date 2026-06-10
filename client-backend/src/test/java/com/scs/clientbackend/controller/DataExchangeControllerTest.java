package com.scs.clientbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.clientbackend.dto.PlaintextDataRequest;
import com.scs.clientbackend.dto.SessionCloseRequest;
import com.scs.clientbackend.exception.ServerConnectionException;
import com.scs.clientbackend.model.ClientSessionContext;
import com.scs.clientbackend.service.InMemoryClientAuthenticationStore;
import com.scs.clientbackend.service.ServerConnection;
import com.scs.crypto.aes.AesEncryptionService;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private InMemoryClientAuthenticationStore authenticationStore;

    @MockBean
    private ServerConnection serverConnection;

    @Test
    void encryptAndSendEncryptsPayloadAndDecryptsServerResponse() throws Exception {
        SecretKey sessionKey = aesKeyService.generateSessionKey();
        String sessionId = seedSession(sessionKey);
        when(serverConnection.sendEncryptedData(any(EncryptedDataRequest.class)))
                .thenReturn(encryptedResponse(sessionId, "server processed: client payload", sessionKey));

        mockMvc.perform(post("/api/client/data/encrypt-and-send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PlaintextDataRequest.builder()
                                .sessionId(sessionId)
                                .plaintext("client payload")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value(sessionId))
                .andExpect(jsonPath("$.encrypted_request.ciphertext").isNotEmpty())
                .andExpect(jsonPath("$.encrypted_response.ciphertext").isNotEmpty())
                .andExpect(jsonPath("$.decrypted_response").value("server processed: client payload"));

        ArgumentCaptor<EncryptedDataRequest> captor = ArgumentCaptor.forClass(EncryptedDataRequest.class);
        verify(serverConnection).sendEncryptedData(captor.capture());
        EncryptedDataRequest sent = captor.getValue();
        byte[] plaintext = aesEncryptionService.decrypt(
                encodingService.decodeBase64(sent.getCiphertext()),
                sessionKey,
                encodingService.decodeBase64(sent.getIv())
        );

        assertThat(new String(plaintext, StandardCharsets.UTF_8)).isEqualTo("client payload");
    }

    @Test
    void encryptAndSendRejectsMissingClientSession() throws Exception {
        mockMvc.perform(post("/api/client/data/encrypt-and-send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PlaintextDataRequest.builder()
                                .sessionId("missing-session")
                                .plaintext("payload")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("DATA_EXCHANGE_FAILED"));
    }

    @Test
    void encryptAndSendReturnsServiceUnavailableWhenServerCallFails() throws Exception {
        String sessionId = seedSession(aesKeyService.generateSessionKey());
        when(serverConnection.sendEncryptedData(any(EncryptedDataRequest.class)))
                .thenThrow(new ServerConnectionException("Failed to send encrypted data to server"));

        mockMvc.perform(post("/api/client/data/encrypt-and-send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PlaintextDataRequest.builder()
                                .sessionId(sessionId)
                                .plaintext("payload")
                                .build())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error_code").value("SERVER_UNAVAILABLE"));
    }

    @Test
    void closeSessionClosesServerAndLocalSession() throws Exception {
        String sessionId = seedSession(aesKeyService.generateSessionKey());
        when(serverConnection.closeServerSession(sessionId))
                .thenReturn(com.scs.clientbackend.dto.SessionCloseResponse.builder()
                        .sessionId(sessionId)
                        .closed(true)
                        .build());

        mockMvc.perform(post("/api/client/session/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SessionCloseRequest.builder()
                                .sessionId(sessionId)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value(sessionId))
                .andExpect(jsonPath("$.closed").value(true));

        assertThat(authenticationStore.findSession(sessionId)).isEmpty();
        verify(serverConnection).closeServerSession(sessionId);
    }

    @Test
    void encryptAndSendRejectsInvalidPayloadBeforeServerCall() throws Exception {
        mockMvc.perform(post("/api/client/data/encrypt-and-send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PlaintextDataRequest.builder()
                                .sessionId("")
                                .plaintext("")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    private String seedSession(SecretKey sessionKey) {
        String sessionId = "session-" + UUID.randomUUID();
        authenticationStore.saveSession(ClientSessionContext.builder()
                .sessionId(sessionId)
                .identityId("identity-1")
                .sessionKey(sessionKey)
                .establishedAt(Instant.now())
                .build());
        return sessionId;
    }

    private EncryptedDataResponse encryptedResponse(String sessionId, String plaintext, SecretKey sessionKey) throws Exception {
        byte[] iv = aesEncryptionService.generateIv();
        byte[] ciphertext = aesEncryptionService.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), sessionKey, iv);
        return EncryptedDataResponse.builder()
                .sessionId(sessionId)
                .ciphertext(encodingService.encodeBase64(ciphertext))
                .iv(encodingService.encodeBase64(iv))
                .build();
    }
}
