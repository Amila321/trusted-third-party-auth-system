package com.scs.server.service;

import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.dto.auth.TtpPublicKeyResponse;
import com.scs.server.exception.TtpClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtpClient {

    private final RestClient restClient;

    @Value("${services.ttp-service.base-url}")
    private String ttpServiceBaseUrl;

    @Value("${services.ttp-service.auth-user-path:/api/ttp/auth/user}")
    private String userAuthenticationPath;

    public TtpAuthenticationDecision authenticateUserForServer(ServerAuthenticationRequest request) {
        String url = ttpServiceBaseUrl + userAuthenticationPath;
        try {
            log.info("Forwarding authentication request to TTP userId={} serverId={}",
                    request.getUserId(),
                    request.getServerId());
            TtpAuthenticationDecision decision = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(TtpAuthenticationDecision.class);

            if (decision == null) {
                throw new TtpClientException("TTP returned an empty authentication decision");
            }
            return decision;
        } catch (TtpClientException e) {
            throw e;
        } catch (Exception e) {
            throw new TtpClientException("Failed to call TTP authentication endpoint", e);
        }
    }

    public TtpPublicKeyResponse getTtpPublicKey() {
        try {
            TtpPublicKeyResponse response = restClient.get()
                    .uri(ttpServiceBaseUrl + "/api/ttp/public-key")
                    .retrieve()
                    .body(TtpPublicKeyResponse.class);
            if (response == null) {
                throw new TtpClientException("TTP returned an empty public key response");
            }
            return response;
        } catch (TtpClientException e) {
            throw e;
        } catch (Exception e) {
            throw new TtpClientException("Failed to fetch TTP public key", e);
        }
    }

    public RegistrationResponse registerServer(RegistrationRequest request) {
        try {
            RegistrationResponse response = restClient.post()
                    .uri(ttpServiceBaseUrl + "/api/ttp/register/server")
                    .body(request)
                    .retrieve()
                    .body(RegistrationResponse.class);
            if (response == null) {
                throw new TtpClientException("TTP returned an empty server registration response");
            }
            return response;
        } catch (TtpClientException e) {
            throw e;
        } catch (Exception e) {
            throw new TtpClientException("Failed to register server with TTP", e);
        }
    }
}
