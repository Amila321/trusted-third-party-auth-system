package com.scs.server.service;

import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.server.exception.TtpClientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class TtpClientTest {

    @Test
    void authenticateUserForServerPostsRequestToConfiguredTtpEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TtpClient client = client(builder.build(), "http://ttp.test", "/api/ttp/auth/user");

        server.expect(once(), requestTo("http://ttp.test/api/ttp/auth/user"))
                .andExpect(method(POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "user_id": "user-1",
                          "server_id": "server-1",
                          "user_certificate_pem": "USER_CERTIFICATE_PEM",
                          "server_certificate_pem": "SERVER_CERTIFICATE_PEM",
                          "challenge": "challenge",
                          "signed_challenge": "signed-challenge"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "authenticated": true,
                          "session_id": "session-1",
                          "encrypted_session_key_for_user": "user-key",
                          "encrypted_session_key_for_server": "server-key",
                          "decided_at": "2026-06-10T12:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        TtpAuthenticationDecision decision = client.authenticateUserForServer(request());

        assertThat(decision.isAuthenticated()).isTrue();
        assertThat(decision.getSessionId()).isEqualTo("session-1");
        assertThat(decision.getEncryptedSessionKeyForServer()).isEqualTo("server-key");
        server.verify();
    }

    @Test
    void authenticateUserForServerThrowsWhenTtpReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TtpClient client = client(builder.build(), "http://ttp.test", "/api/ttp/auth/user");

        server.expect(once(), requestTo("http://ttp.test/api/ttp/auth/user"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.authenticateUserForServer(request()))
                .isInstanceOf(TtpClientException.class)
                .hasMessageContaining("Failed to call TTP authentication endpoint");
        server.verify();
    }

    private TtpClient client(RestClient restClient, String baseUrl, String path) {
        TtpClient client = new TtpClient(restClient);
        ReflectionTestUtils.setField(client, "ttpServiceBaseUrl", baseUrl);
        ReflectionTestUtils.setField(client, "userAuthenticationPath", path);
        return client;
    }

    private ServerAuthenticationRequest request() {
        return ServerAuthenticationRequest.builder()
                .userId("user-1")
                .serverId("server-1")
                .userCertificatePem("USER_CERTIFICATE_PEM")
                .serverCertificatePem("SERVER_CERTIFICATE_PEM")
                .challenge("challenge")
                .signedChallenge("signed-challenge")
                .build();
    }
}
