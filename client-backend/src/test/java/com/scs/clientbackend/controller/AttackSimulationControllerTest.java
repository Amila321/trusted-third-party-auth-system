package com.scs.clientbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.clientbackend.model.ClientIdentityContext;
import com.scs.clientbackend.service.InMemoryClientAuthenticationStore;
import com.scs.clientbackend.service.ServerConnection;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.attack.AttackScenarioType;
import com.scs.dto.attack.AttackSimulationRequest;
import com.scs.dto.auth.IdentityStatusResponse;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AttackSimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RsaKeyService rsaKeyService;

    @Autowired
    private InMemoryClientAuthenticationStore authenticationStore;

    @MockBean
    private ServerConnection serverConnection;

    @Test
    void forgedUserCertificateAttackReturnsRejectedDecisionAndDoesNotStoreSession() throws Exception {
        seedUser();
        when(serverConnection.getServerIdentity()).thenReturn(IdentityStatusResponse.builder()
                .identityId("server-1")
                .identityName("server")
                .registered(true)
                .build());
        when(serverConnection.requestSession(any(UserAuthenticationRequest.class))).thenReturn(TtpAuthenticationDecision.builder()
                .authenticated(false)
                .rejectionReason("Certificate was not signed by the TTP")
                .decidedAt("2026-06-10T12:00:00Z")
                .build());

        mockMvc.perform(post("/api/client/attack/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AttackSimulationRequest.builder()
                                .scenario(AttackScenarioType.FORGED_USER_CERTIFICATE)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("FORGED_USER_CERTIFICATE"))
                .andExpect(jsonPath("$.rejected").value(true))
                .andExpect(jsonPath("$.ttp_decision.authenticated").value(false))
                .andExpect(jsonPath("$.ttp_decision.rejection_reason").value("Certificate was not signed by the TTP"))
                .andExpect(jsonPath("$.evidence.session_created").value(false));

        ArgumentCaptor<UserAuthenticationRequest> captor = ArgumentCaptor.forClass(UserAuthenticationRequest.class);
        verify(serverConnection).requestSession(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(authenticationStore.findSession("attack-session")).isEmpty();
    }

    @Test
    void invalidChallengeSignatureAttackUsesRealCertificateButRejectedDecision() throws Exception {
        seedUser();
        when(serverConnection.getServerIdentity()).thenReturn(IdentityStatusResponse.builder()
                .identityId("server-1")
                .identityName("server")
                .registered(true)
                .build());
        when(serverConnection.requestSession(any(UserAuthenticationRequest.class))).thenReturn(TtpAuthenticationDecision.builder()
                .authenticated(false)
                .rejectionReason("Invalid signed challenge")
                .decidedAt("2026-06-10T12:00:00Z")
                .build());

        mockMvc.perform(post("/api/client/attack/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AttackSimulationRequest.builder()
                                .scenario(AttackScenarioType.INVALID_CHALLENGE_SIGNATURE)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected").value(true))
                .andExpect(jsonPath("$.evidence.used_real_user_certificate").value(true))
                .andExpect(jsonPath("$.evidence.used_invalid_signature").value(true));
    }

    private void seedUser() throws Exception {
        authenticationStore.saveIdentity(ClientIdentityContext.builder()
                .identityId("user-1")
                .identityName("alice")
                .keyPair(rsaKeyService.generateKeyPair())
                .publicKeyPem("PUBLIC_KEY_PEM")
                .certificatePem("USER_CERTIFICATE_PEM")
                .registeredAt(Instant.now().toString())
                .build());
    }
}
