package com.scs.clientbackend.service;

import com.scs.clientbackend.dto.SessionCloseRequest;
import com.scs.clientbackend.dto.SessionCloseResponse;
import com.scs.clientbackend.exception.ServerConnectionException;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerConnection {

    private final RestClient restClient;

    @Value("${services.server-service.base-url}")
    private String serverServiceBaseUrl;

    public EncryptedDataResponse sendEncryptedData(EncryptedDataRequest request) {
        try {
            log.info("Sending encrypted payload to server sessionId={}", request.getSessionId());
            EncryptedDataResponse response = restClient.post()
                    .uri(serverServiceBaseUrl + "/api/server/data/decrypt-and-process")
                    .body(request)
                    .retrieve()
                    .body(EncryptedDataResponse.class);
            if (response == null) {
                throw new ServerConnectionException("Server returned an empty encrypted data response");
            }
            return response;
        } catch (ServerConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConnectionException("Failed to send encrypted data to server", e);
        }
    }

    public SessionCloseResponse closeServerSession(String sessionId) {
        try {
            SessionCloseResponse response = restClient.post()
                    .uri(serverServiceBaseUrl + "/api/server/session/close")
                    .body(SessionCloseRequest.builder().sessionId(sessionId).build())
                    .retrieve()
                    .body(SessionCloseResponse.class);
            if (response == null) {
                throw new ServerConnectionException("Server returned an empty session close response");
            }
            return response;
        } catch (ServerConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConnectionException("Failed to close server session", e);
        }
    }
}
