package com.scs.clientbackend.service;

import com.scs.clientbackend.exception.TtpClientException;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TtpClientTest {

    @Test
    void registerUserPostsRegistrationRequestToTtp() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TtpClient client = client(builder.build(), "http://ttp.test");

        server.expect(once(), requestTo("http://ttp.test/api/ttp/register/user"))
                .andExpect(method(POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "identity_name": "alice",
                          "public_key_pem": "PUBLIC_KEY_PEM"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "identity_id": "user-1",
                          "certificate_pem": "CERTIFICATE_PEM",
                          "registered_at": "2026-06-10T12:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        RegistrationResponse response = client.registerUser(RegistrationRequest.builder()
                .identityName("alice")
                .publicKeyPem("PUBLIC_KEY_PEM")
                .build());

        assertThat(response.getIdentityId()).isEqualTo("user-1");
        assertThat(response.getCertificatePem()).isEqualTo("CERTIFICATE_PEM");
        server.verify();
    }

    @Test
    void registerUserThrowsWhenTtpReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TtpClient client = client(builder.build(), "http://ttp.test");

        server.expect(once(), requestTo("http://ttp.test/api/ttp/register/user"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.registerUser(RegistrationRequest.builder()
                        .identityName("alice")
                        .publicKeyPem("PUBLIC_KEY_PEM")
                        .build()))
                .isInstanceOf(TtpClientException.class)
                .hasMessageContaining("Failed to register user with TTP");
        server.verify();
    }

    private TtpClient client(RestClient restClient, String baseUrl) {
        TtpClient client = new TtpClient(restClient);
        ReflectionTestUtils.setField(client, "ttpServiceBaseUrl", baseUrl);
        return client;
    }
}
