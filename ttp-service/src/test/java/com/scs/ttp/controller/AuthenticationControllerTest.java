package com.scs.ttp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.crypto.config.CryptoConstants;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.hash.HashService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.ttp.service.InMemorySessionStore;
import com.scs.ttp.service.TtpCertificateAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private EncodingService encodingService;

    @Autowired
    private HashService hashService;

    @Autowired
    private TtpCertificateAuthority certificateAuthority;

    @Autowired
    private InMemorySessionStore sessionStore;

    @Test
    void authenticateUserForServerAcceptsValidCertificatesAndSignedChallenge() throws Exception {
        RegisteredTestIdentity user = register("phase6-user-" + UUID.randomUUID(), "/api/ttp/register/user");
        RegisteredTestIdentity server = register("phase6-server-" + UUID.randomUUID(), "/api/ttp/register/server");
        String challenge = "challenge-" + UUID.randomUUID();

        String responseBody = mockMvc.perform(post("/api/ttp/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest(user, server, challenge, sign(challenge, user.keyPair())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.session_id").isNotEmpty())
                .andExpect(jsonPath("$.encrypted_session_key_for_user").isNotEmpty())
                .andExpect(jsonPath("$.encrypted_session_key_for_server").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode decision = objectMapper.readTree(responseBody);
        String sessionId = decision.get("session_id").asText();
        byte[] userSessionKey = rsaEncryptionService.decrypt(
                encodingService.decodeBase64(decision.get("encrypted_session_key_for_user").asText()),
                user.keyPair().getPrivate()
        );
        byte[] serverSessionKey = rsaEncryptionService.decrypt(
                encodingService.decodeBase64(decision.get("encrypted_session_key_for_server").asText()),
                server.keyPair().getPrivate()
        );

        assertThat(userSessionKey).hasSize(32);
        assertThat(serverSessionKey).containsExactly(userSessionKey);
        assertThat(sessionStore.getSession(sessionId)).isPresent();
    }

    @Test
    void authenticateUserForServerRejectsInvalidSignature() throws Exception {
        RegisteredTestIdentity user = register("phase6-bad-signature-user-" + UUID.randomUUID(), "/api/ttp/register/user");
        RegisteredTestIdentity server = register("phase6-bad-signature-server-" + UUID.randomUUID(), "/api/ttp/register/server");

        mockMvc.perform(post("/api/ttp/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest(user, server, "challenge", "not-valid-base64"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.rejection_reason").value("Invalid signed challenge"));
    }

    @Test
    void authenticateUserForServerRejectsUnknownIdentity() throws Exception {
        RegisteredTestIdentity user = register("phase6-known-user-" + UUID.randomUUID(), "/api/ttp/register/user");
        RegisteredTestIdentity server = register("phase6-known-server-" + UUID.randomUUID(), "/api/ttp/register/server");
        String challenge = "challenge";

        ServerAuthenticationRequest request = authRequest(user, server, challenge, sign(challenge, user.keyPair()));
        request.setUserId("missing-user-id");

        mockMvc.perform(post("/api/ttp/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.rejection_reason").value("Identity not found: missing-user-id"));
    }

    @Test
    void authenticateUserForServerRejectsCertificateMismatch() throws Exception {
        RegisteredTestIdentity user = register("phase6-mismatch-user-" + UUID.randomUUID(), "/api/ttp/register/user");
        RegisteredTestIdentity otherUser = register("phase6-mismatch-other-" + UUID.randomUUID(), "/api/ttp/register/user");
        RegisteredTestIdentity server = register("phase6-mismatch-server-" + UUID.randomUUID(), "/api/ttp/register/server");
        String challenge = "challenge";

        ServerAuthenticationRequest request = authRequest(user, server, challenge, sign(challenge, user.keyPair()));
        request.setUserCertificatePem(otherUser.certificatePem());

        mockMvc.perform(post("/api/ttp/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.rejection_reason").value("Presented certificate does not match registered identity"));
    }

    private RegisteredTestIdentity register(String name, String path) throws Exception {
        KeyPair keyPair = rsaKeyService.generateKeyPair();
        String identityId = hashService.hashIdentity(name.trim().toLowerCase(java.util.Locale.ROOT));
        RegistrationRequest request = RegistrationRequest.builder()
                .identityName(name)
                .encryptedIdentityId(encodingService.encodeBase64(rsaEncryptionService.encrypt(
                        identityId.getBytes(StandardCharsets.UTF_8),
                        certificateAuthority.getTtpKeyPair().getPublic()
                )))
                .publicKeyPem(rsaKeyService.encodePublicKeyPem(keyPair.getPublic()))
                .build();

        String responseBody = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        return new RegisteredTestIdentity(
                response.get("identity_id").asText(),
                response.get("certificate_pem").asText(),
                keyPair
        );
    }

    private ServerAuthenticationRequest authRequest(
            RegisteredTestIdentity user,
            RegisteredTestIdentity server,
            String challenge,
            String signedChallenge
    ) {
        return ServerAuthenticationRequest.builder()
                .userId(user.identityId())
                .serverId(server.identityId())
                .userCertificatePem(user.certificatePem())
                .serverCertificatePem(server.certificatePem())
                .challenge(challenge)
                .signedChallenge(signedChallenge)
                .build();
    }

    private String sign(String challenge, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance(CryptoConstants.SIGNATURE_ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(challenge.getBytes(StandardCharsets.UTF_8));
        return encodingService.encodeBase64(signature.sign());
    }

    private record RegisteredTestIdentity(String identityId, String certificatePem, KeyPair keyPair) {
    }
}
