package com.scs.clientbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.clientbackend.dto.ClientAuthenticationInitiateRequest;
import com.scs.clientbackend.exception.TtpClientException;
import com.scs.clientbackend.service.TtpClient;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.dto.auth.TtpPublicKeyResponse;
import com.scs.dto.session.SessionKeyResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void initiateGeneratesClientKeyPairAndRegistersUserWithTtp() throws Exception {
        mockTtpPublicKey();
        when(ttpClient.registerUser(any(RegistrationRequest.class))).thenReturn(RegistrationResponse.builder()
                .identityId("client-user-1")
                .certificatePem("CERTIFICATE_PEM")
                .registeredAt("2026-06-10T12:00:00Z")
                .build());

        mockMvc.perform(post("/api/client/auth/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ClientAuthenticationInitiateRequest.builder()
                                .identityName("alice")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity_id").value("client-user-1"))
                .andExpect(jsonPath("$.certificate_pem").value("CERTIFICATE_PEM"))
                .andExpect(jsonPath("$.public_key_pem").isNotEmpty());

        ArgumentCaptor<RegistrationRequest> captor = ArgumentCaptor.forClass(RegistrationRequest.class);
        verify(ttpClient).registerUser(captor.capture());
        assertThat(captor.getValue().getIdentityName()).isEqualTo("alice");
        assertThat(captor.getValue().getEncryptedIdentityId()).isNotBlank();
        assertThat(captor.getValue().getPublicKeyPem()).contains("BEGIN PUBLIC KEY");
    }

    @Test
    void completeDecryptsAndStoresSessionKeyForActiveIdentity() throws Exception {
        mockTtpPublicKey();
        when(ttpClient.registerUser(any(RegistrationRequest.class))).thenReturn(RegistrationResponse.builder()
                .identityId("client-user-2")
                .certificatePem("CERTIFICATE_PEM")
                .registeredAt("2026-06-10T12:00:00Z")
                .build());

        String initiateBody = mockMvc.perform(post("/api/client/auth/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ClientAuthenticationInitiateRequest.builder()
                                .identityName("bob")
                                .build())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode initiate = objectMapper.readTree(initiateBody);
        PublicKey clientPublicKey = rsaKeyService.decodePublicKeyPem(initiate.get("public_key_pem").asText());
        SecretKey sessionKey = aesKeyService.generateSessionKey();
        String encryptedSessionKey = encodingService.encodeBase64(
                rsaEncryptionService.encrypt(sessionKey.getEncoded(), clientPublicKey)
        );

        mockMvc.perform(post("/api/client/auth/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SessionKeyResponse.builder()
                                .sessionId("session-1")
                                .encryptedSessionKey(encryptedSessionKey)
                                .issuedAt("2026-06-10T12:01:00Z")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value("session-1"))
                .andExpect(jsonPath("$.identity_id").value("client-user-2"))
                .andExpect(jsonPath("$.session_key_base64").value(aesKeyService.encodeKey(sessionKey)))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void initiateReturnsServiceUnavailableWhenTtpRegistrationFails() throws Exception {
        mockTtpPublicKey();
        when(ttpClient.registerUser(any(RegistrationRequest.class)))
                .thenThrow(new TtpClientException("Failed to register user with TTP"));

        mockMvc.perform(post("/api/client/auth/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ClientAuthenticationInitiateRequest.builder()
                                .identityName("carol")
                                .build())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error_code").value("TTP_UNAVAILABLE"));
    }

    @Test
    void initiateRejectsBlankIdentityNameBeforeCallingTtp() throws Exception {
        mockMvc.perform(post("/api/client/auth/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ClientAuthenticationInitiateRequest.builder()
                                .identityName("")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));

        verify(ttpClient, never()).registerUser(any());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void completeFailsWhenNoIdentityWasInitiated() throws Exception {
        SecretKey sessionKey = aesKeyService.generateSessionKey();

        mockMvc.perform(post("/api/client/auth/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SessionKeyResponse.builder()
                                .sessionId("session-" + UUID.randomUUID())
                                .encryptedSessionKey(aesKeyService.encodeKey(sessionKey))
                                .issuedAt("2026-06-10T12:01:00Z")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("CLIENT_AUTHENTICATION_FAILED"));
    }

    private void mockTtpPublicKey() throws Exception {
        KeyPair keyPair = rsaKeyService.generateKeyPair();
        when(ttpClient.getTtpPublicKey()).thenReturn(TtpPublicKeyResponse.builder()
                .publicKeyPem(rsaKeyService.encodePublicKeyPem(keyPair.getPublic()))
                .issuedAt("2026-06-10T12:00:00Z")
                .build());
    }
}
