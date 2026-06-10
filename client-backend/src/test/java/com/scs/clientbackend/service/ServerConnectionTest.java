package com.scs.clientbackend.service;

import com.scs.clientbackend.exception.ServerConnectionException;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
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

class ServerConnectionTest {

    @Test
    void sendEncryptedDataPostsToServerEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ServerConnection connection = connection(builder.build(), "http://server.test");

        server.expect(once(), requestTo("http://server.test/api/server/data/decrypt-and-process"))
                .andExpect(method(POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "session_id": "session-1",
                          "ciphertext": "ciphertext",
                          "iv": "iv"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "session_id": "session-1",
                          "ciphertext": "response-ciphertext",
                          "iv": "response-iv"
                        }
                        """, MediaType.APPLICATION_JSON));

        EncryptedDataResponse response = connection.sendEncryptedData(EncryptedDataRequest.builder()
                .sessionId("session-1")
                .ciphertext("ciphertext")
                .iv("iv")
                .build());

        assertThat(response.getCiphertext()).isEqualTo("response-ciphertext");
        server.verify();
    }

    @Test
    void sendEncryptedDataThrowsWhenServerReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ServerConnection connection = connection(builder.build(), "http://server.test");

        server.expect(once(), requestTo("http://server.test/api/server/data/decrypt-and-process"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> connection.sendEncryptedData(EncryptedDataRequest.builder()
                        .sessionId("session-1")
                        .ciphertext("ciphertext")
                        .iv("iv")
                        .build()))
                .isInstanceOf(ServerConnectionException.class)
                .hasMessageContaining("Failed to send encrypted data to server");
        server.verify();
    }

    @Test
    void closeServerSessionPostsToCloseEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ServerConnection connection = connection(builder.build(), "http://server.test");

        server.expect(once(), requestTo("http://server.test/api/server/session/close"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "session_id": "session-1"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "session_id": "session-1",
                          "closed": true
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(connection.closeServerSession("session-1").isClosed()).isTrue();
        server.verify();
    }

    private ServerConnection connection(RestClient restClient, String baseUrl) {
        ServerConnection connection = new ServerConnection(restClient);
        ReflectionTestUtils.setField(connection, "serverServiceBaseUrl", baseUrl);
        return connection;
    }
}
