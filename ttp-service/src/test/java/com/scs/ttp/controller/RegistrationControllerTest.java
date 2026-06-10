package com.scs.ttp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.crypto.certificate.CertificateService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.hash.HashService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.ttp.service.TtpCertificateAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationControllerTest {

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
    private CertificateService certificateService;

    @Autowired
    private TtpCertificateAuthority certificateAuthority;

    @Test
    void registerUserIssuesTtpSignedCertificate() throws Exception {
        RegistrationRequest request = registrationRequest("phase4-user-" + UUID.randomUUID());

        String responseBody = mockMvc.perform(post("/api/ttp/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identity_id").isNotEmpty())
                .andExpect(jsonPath("$.certificate_pem").isNotEmpty())
                .andExpect(jsonPath("$.registered_at").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        X509Certificate certificate = certificateService.decodeCertificatePem(response.get("certificate_pem").asText());

        assertThat(certificateService.validateCertificate(certificate, certificateAuthority.getTtpKeyPair().getPublic()))
                .isTrue();
        assertThat(certificateService.extractSubjectDN(certificate)).contains("OU=USER");
    }

    @Test
    void registerServerIssuesServerCertificate() throws Exception {
        RegistrationRequest request = registrationRequest("phase4-server-" + UUID.randomUUID());

        String responseBody = mockMvc.perform(post("/api/ttp/register/server")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identity_id").isNotEmpty())
                .andExpect(jsonPath("$.certificate_pem").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        X509Certificate certificate = certificateService.decodeCertificatePem(response.get("certificate_pem").asText());

        assertThat(certificateService.extractSubjectDN(certificate)).contains("OU=SERVER");
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        RegistrationRequest request = registrationRequest("phase4-duplicate-" + UUID.randomUUID());

        mockMvc.perform(post("/api/ttp/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/ttp/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("DUPLICATE_IDENTITY"));
    }

    @Test
    void certificateCanBeRetrievedByIdentityId() throws Exception {
        RegistrationRequest request = registrationRequest("phase4-lookup-" + UUID.randomUUID());

        String registrationBody = mockMvc.perform(post("/api/ttp/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registration = objectMapper.readTree(registrationBody);
        String identityId = registration.get("identity_id").asText();

        mockMvc.perform(get("/api/ttp/certificate/{identityId}", identityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity_id").value(identityId))
                .andExpect(jsonPath("$.certificate_pem").value(registration.get("certificate_pem").asText()))
                .andExpect(jsonPath("$.registered_at").value(registration.get("registered_at").asText()));
    }

    @Test
    void missingCertificateReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/ttp/certificate/{identityId}", "missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("IDENTITY_NOT_FOUND"));
    }

    private RegistrationRequest registrationRequest(String identityName) throws Exception {
        KeyPair keyPair = rsaKeyService.generateKeyPair();
        String identityId = hashService.hashIdentity(identityName.trim().toLowerCase(java.util.Locale.ROOT));
        return RegistrationRequest.builder()
                .identityName(identityName)
                .encryptedIdentityId(encodingService.encodeBase64(rsaEncryptionService.encrypt(
                        identityId.getBytes(StandardCharsets.UTF_8),
                        certificateAuthority.getTtpKeyPair().getPublic()
                )))
                .publicKeyPem(rsaKeyService.encodePublicKeyPem(keyPair.getPublic()))
                .build();
    }
}
