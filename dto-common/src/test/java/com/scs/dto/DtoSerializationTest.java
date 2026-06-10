package com.scs.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.dto.auth.*;
import com.scs.dto.common.ErrorResponse;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
import com.scs.dto.session.SessionKeyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void registrationRequest_roundTrip() throws Exception {
        RegistrationRequest original = RegistrationRequest.builder()
                .identityName("alice")
                .encryptedIdentityId("encryptedIdentityId==")
                .publicKeyPem("-----BEGIN PUBLIC KEY-----\nMIIB...\n-----END PUBLIC KEY-----")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"identity_name\"").contains("\"encrypted_identity_id\"").contains("\"public_key_pem\"");

        RegistrationRequest deserialized = objectMapper.readValue(json, RegistrationRequest.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void registrationResponse_roundTrip() throws Exception {
        RegistrationResponse original = RegistrationResponse.builder()
                .identityId("abc123")
                .certificatePem("-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----")
                .registeredAt("2026-06-09T12:00:00Z")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"identity_id\"").contains("\"certificate_pem\"").contains("\"registered_at\"");

        RegistrationResponse deserialized = objectMapper.readValue(json, RegistrationResponse.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void loginRequest_roundTrip() throws Exception {
        LoginRequest original = LoginRequest.builder()
                .identityId("abc123")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"identity_id\"");

        LoginRequest deserialized = objectMapper.readValue(json, LoginRequest.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void loginResponse_roundTrip() throws Exception {
        LoginResponse original = LoginResponse.builder()
                .identityId("abc123")
                .certificatePem("-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----")
                .challenge("randomChallenge==")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"identity_id\"").contains("\"certificate_pem\"").contains("\"challenge\"");

        LoginResponse deserialized = objectMapper.readValue(json, LoginResponse.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void userAuthenticationRequest_roundTrip() throws Exception {
        UserAuthenticationRequest original = UserAuthenticationRequest.builder()
                .userId("user-abc")
                .serverId("server-xyz")
                .userCertificatePem("-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----")
                .challenge("challengeBase64==")
                .signedChallenge("signedBase64==")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"user_id\"")
                .contains("\"server_id\"")
                .contains("\"user_certificate_pem\"")
                .contains("\"challenge\"")
                .contains("\"signed_challenge\"");

        UserAuthenticationRequest deserialized = objectMapper.readValue(json, UserAuthenticationRequest.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void serverAuthenticationRequest_roundTrip() throws Exception {
        ServerAuthenticationRequest original = ServerAuthenticationRequest.builder()
                .userId("user-abc")
                .serverId("server-xyz")
                .userCertificatePem("-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----")
                .serverCertificatePem("-----BEGIN CERTIFICATE-----\nMIIS...\n-----END CERTIFICATE-----")
                .challenge("challengeBase64==")
                .signedChallenge("signedBase64==")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"user_id\"")
                .contains("\"server_id\"")
                .contains("\"user_certificate_pem\"")
                .contains("\"server_certificate_pem\"")
                .contains("\"challenge\"")
                .contains("\"signed_challenge\"");

        ServerAuthenticationRequest deserialized = objectMapper.readValue(json, ServerAuthenticationRequest.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void ttpAuthenticationDecision_roundTrip_authenticated() throws Exception {
        TtpAuthenticationDecision original = TtpAuthenticationDecision.builder()
                .authenticated(true)
                .sessionId("sess-001")
                .encryptedSessionKeyForUser("encKeyUser==")
                .encryptedSessionKeyForServer("encKeyServer==")
                .rejectionReason(null)
                .decidedAt("2026-06-09T12:00:00Z")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"authenticated\":true")
                .contains("\"session_id\"")
                .contains("\"encrypted_session_key_for_user\"")
                .contains("\"encrypted_session_key_for_server\"")
                .contains("\"decided_at\"");

        TtpAuthenticationDecision deserialized = objectMapper.readValue(json, TtpAuthenticationDecision.class);
        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.isAuthenticated()).isTrue();
    }

    @Test
    void ttpAuthenticationDecision_roundTrip_rejected() throws Exception {
        TtpAuthenticationDecision original = TtpAuthenticationDecision.builder()
                .authenticated(false)
                .sessionId(null)
                .encryptedSessionKeyForUser(null)
                .encryptedSessionKeyForServer(null)
                .rejectionReason("Certificate signature invalid")
                .decidedAt("2026-06-09T12:00:00Z")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"authenticated\":false")
                .contains("\"rejection_reason\":\"Certificate signature invalid\"");

        TtpAuthenticationDecision deserialized = objectMapper.readValue(json, TtpAuthenticationDecision.class);
        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.isAuthenticated()).isFalse();
        assertThat(deserialized.getRejectionReason()).isEqualTo("Certificate signature invalid");
    }

    @Test
    void sessionKeyResponse_roundTrip() throws Exception {
        SessionKeyResponse original = SessionKeyResponse.builder()
                .sessionId("sess-001")
                .encryptedSessionKey("encAesKey==")
                .issuedAt("2026-06-09T12:00:00Z")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"session_id\"")
                .contains("\"encrypted_session_key\"")
                .contains("\"issued_at\"");

        SessionKeyResponse deserialized = objectMapper.readValue(json, SessionKeyResponse.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void encryptedDataRequest_roundTrip() throws Exception {
        EncryptedDataRequest original = EncryptedDataRequest.builder()
                .sessionId("sess-001")
                .ciphertext("ciphertextBase64==")
                .iv("ivBase64==")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"session_id\"")
                .contains("\"ciphertext\"")
                .contains("\"iv\"");

        EncryptedDataRequest deserialized = objectMapper.readValue(json, EncryptedDataRequest.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void encryptedDataResponse_roundTrip() throws Exception {
        EncryptedDataResponse original = EncryptedDataResponse.builder()
                .sessionId("sess-001")
                .ciphertext("responseCiphertextBase64==")
                .iv("responseIvBase64==")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"session_id\"")
                .contains("\"ciphertext\"")
                .contains("\"iv\"");

        EncryptedDataResponse deserialized = objectMapper.readValue(json, EncryptedDataResponse.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void errorResponse_roundTrip() throws Exception {
        ErrorResponse original = ErrorResponse.builder()
                .errorCode("CERT_INVALID")
                .message("Certificate validation failed")
                .details("Signature does not match TTP public key")
                .timestamp("2026-06-09T12:00:00Z")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json)
                .contains("\"error_code\"")
                .contains("\"message\"")
                .contains("\"details\"")
                .contains("\"timestamp\"");

        ErrorResponse deserialized = objectMapper.readValue(json, ErrorResponse.class);
        assertThat(deserialized).isEqualTo(original);
    }
}
