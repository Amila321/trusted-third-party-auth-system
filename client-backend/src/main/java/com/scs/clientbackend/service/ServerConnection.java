package com.scs.clientbackend.service;

import com.scs.clientbackend.dto.SessionCloseRequest;
import com.scs.clientbackend.dto.SessionCloseResponse;
import com.scs.clientbackend.exception.ServerConnectionException;
import com.scs.dto.auth.IdentityRegistrationRequest;
import com.scs.dto.auth.IdentityStatusResponse;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
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

    public IdentityStatusResponse registerServer(IdentityRegistrationRequest request) {
        try {
            IdentityStatusResponse response = restClient.post()
                    .uri(serverServiceBaseUrl + "/api/server/auth/register")
                    .body(request)
                    .retrieve()
                    .body(IdentityStatusResponse.class);
            if (response == null) {
                throw new ServerConnectionException("Server returned an empty identity registration response");
            }
            return response;
        } catch (ServerConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConnectionException("Failed to register server identity", e);
        }
    }

    public IdentityStatusResponse getServerIdentity() {
        try {
            IdentityStatusResponse response = restClient.get()
                    .uri(serverServiceBaseUrl + "/api/server/auth/identity")
                    .retrieve()
                    .body(IdentityStatusResponse.class);
            if (response == null) {
                throw new ServerConnectionException("Server returned an empty identity status response");
            }
            return response;
        } catch (ServerConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConnectionException("Failed to fetch server identity", e);
        }
    }

    public TtpAuthenticationDecision requestSession(UserAuthenticationRequest request) {
        try {
            TtpAuthenticationDecision response = restClient.post()
                    .uri(serverServiceBaseUrl + "/api/server/request")
                    .body(request)
                    .retrieve()
                    .body(TtpAuthenticationDecision.class);
            if (response == null) {
                throw new ServerConnectionException("Server returned an empty TTP authentication decision");
            }
            return response;
        } catch (ServerConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerConnectionException("Failed to request session through server", e);
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
