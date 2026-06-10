package com.scs.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
import com.scs.server.exception.TtpClientException;
import com.scs.server.model.ServerSessionContext;
import com.scs.server.model.ServerIdentityContext;
import com.scs.server.service.InMemoryServerIdentityStore;
import com.scs.server.service.InMemoryServerSessionStore;
import com.scs.server.service.TtpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.security.KeyPair;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "server.identity.certificate-pem=SERVER_CERTIFICATE_PEM")
@AutoConfigureMockMvc
class ServiceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryServerSessionStore sessionStore;

    @Autowired
    private InMemoryServerIdentityStore identityStore;

    @Autowired
    private RsaKeyService rsaKeyService;

    @Autowired
    private RsaEncryptionService rsaEncryptionService;

    @Autowired
    private AesKeyService aesKeyService;

    @Autowired
    private EncodingService encodingService;

    @MockBean
    private TtpClient ttpClient;

    @Test
    void requestServiceForwardsUserAuthenticationToTtpAndStoresAcceptedSession() throws Exception {
        KeyPair serverKeyPair = seedServerIdentity("server-1");
        UserAuthenticationRequest request = validRequest("user-1", "server-1");
        when(ttpClient.authenticateUserForServer(any(ServerAuthenticationRequest.class)))
                .thenReturn(acceptedDecision("session-1", serverKeyPair));

        mockMvc.perform(post("/api/server/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.session_id").value("session-1"))
                .andExpect(jsonPath("$.encrypted_session_key_for_server").isNotEmpty());

        ArgumentCaptor<ServerAuthenticationRequest> captor = ArgumentCaptor.forClass(ServerAuthenticationRequest.class);
        verify(ttpClient).authenticateUserForServer(captor.capture());
        ServerAuthenticationRequest ttpRequest = captor.getValue();

        assertThat(ttpRequest.getUserId()).isEqualTo("user-1");
        assertThat(ttpRequest.getServerId()).isEqualTo("server-1");
        assertThat(ttpRequest.getUserCertificatePem()).isEqualTo("USER_CERTIFICATE_PEM");
        assertThat(ttpRequest.getServerCertificatePem()).isEqualTo("SERVER_CERTIFICATE_PEM");
        assertThat(ttpRequest.getChallenge()).isEqualTo("challenge");
        assertThat(ttpRequest.getSignedChallenge()).isEqualTo("signed-challenge");

        Optional<ServerSessionContext> storedSession = sessionStore.findBySessionId("session-1");
        assertThat(storedSession).isPresent();
        assertThat(storedSession.get().getEncryptedSessionKeyForServer()).isNotBlank();
        assertThat(storedSession.get().getSessionKey()).isNotNull();
    }

    @Test
    void requestServiceReturnsRejectedTtpDecisionWithoutStoringSession() throws Exception {
        seedServerIdentity("server-1");
        UserAuthenticationRequest request = validRequest("user-2", "server-1");
        when(ttpClient.authenticateUserForServer(any(ServerAuthenticationRequest.class)))
                .thenReturn(rejectedDecision("invalid signature"));

        mockMvc.perform(post("/api/server/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.rejection_reason").value("invalid signature"));

        assertThat(sessionStore.findBySessionId("rejected-session")).isEmpty();
    }

    @Test
    void requestServiceReturnsServiceUnavailableWhenTtpCannotBeReached() throws Exception {
        seedServerIdentity("server-1");
        when(ttpClient.authenticateUserForServer(any(ServerAuthenticationRequest.class)))
                .thenThrow(new TtpClientException("Failed to call TTP authentication endpoint"));

        mockMvc.perform(post("/api/server/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("user-3", "server-1"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error_code").value("TTP_UNAVAILABLE"));
    }

    @Test
    void requestServiceRejectsInvalidPayloadBeforeCallingTtp() throws Exception {
        UserAuthenticationRequest request = validRequest("", "server-1");

        mockMvc.perform(post("/api/server/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));

        verify(ttpClient, never()).authenticateUserForServer(any(ServerAuthenticationRequest.class));
    }

    @Test
    void authenticatedDecisionWithoutSessionIdReturnsBadGateway() throws Exception {
        KeyPair serverKeyPair = seedServerIdentity("server-1");
        TtpAuthenticationDecision decision = acceptedDecision("", serverKeyPair);
        when(ttpClient.authenticateUserForServer(any(ServerAuthenticationRequest.class)))
                .thenReturn(decision);

        mockMvc.perform(post("/api/server/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("user-4", "server-1"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error_code").value("INVALID_TTP_DECISION"));
    }

    private UserAuthenticationRequest validRequest(String userId, String serverId) {
        return UserAuthenticationRequest.builder()
                .userId(userId)
                .serverId(serverId)
                .userCertificatePem("USER_CERTIFICATE_PEM")
                .challenge("challenge")
                .signedChallenge("signed-challenge")
                .build();
    }

    private TtpAuthenticationDecision acceptedDecision(String sessionId, KeyPair serverKeyPair) throws Exception {
        return TtpAuthenticationDecision.builder()
                .authenticated(true)
                .sessionId(sessionId)
                .encryptedSessionKeyForUser("user-key")
                .encryptedSessionKeyForServer(encodingService.encodeBase64(
                        rsaEncryptionService.encrypt(aesKeyService.generateSessionKey().getEncoded(), serverKeyPair.getPublic())
                ))
                .decidedAt("2026-06-10T12:00:00Z")
                .build();
    }

    private KeyPair seedServerIdentity(String serverId) throws Exception {
        KeyPair keyPair = rsaKeyService.generateKeyPair();
        identityStore.save(ServerIdentityContext.builder()
                .identityId(serverId)
                .identityName("server")
                .keyPair(keyPair)
                .publicKeyPem(rsaKeyService.encodePublicKeyPem(keyPair.getPublic()))
                .certificatePem("SERVER_CERTIFICATE_PEM")
                .registeredAt(Instant.now().toString())
                .build());
        return keyPair;
    }

    private TtpAuthenticationDecision rejectedDecision(String reason) {
        return TtpAuthenticationDecision.builder()
                .authenticated(false)
                .sessionId("rejected-session")
                .rejectionReason(reason)
                .decidedAt("2026-06-10T12:00:00Z")
                .build();
    }
}
