package com.scs.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.dto.auth.UserAuthenticationRequest;
import com.scs.server.service.TtpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "server.identity.certificate-pem=")
@AutoConfigureMockMvc
class ServiceRequestControllerConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TtpClient ttpClient;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void requestServiceFailsFastWhenServerCertificateIsNotConfigured() throws Exception {
        UserAuthenticationRequest request = UserAuthenticationRequest.builder()
                .userId("user-1")
                .serverId("server-1")
                .userCertificatePem("USER_CERTIFICATE_PEM")
                .challenge("challenge")
                .signedChallenge("signed-challenge")
                .build();

        mockMvc.perform(post("/api/server/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_code").value("SERVER_CONFIGURATION_ERROR"));

        verify(ttpClient, never()).authenticateUserForServer(any());
    }
}
