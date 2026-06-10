package com.scs.clientbackend.service;

import com.scs.clientbackend.exception.TtpClientException;
import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.dto.auth.TtpPublicKeyResponse;
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

    public RegistrationResponse registerUser(RegistrationRequest request) {
        try {
            log.info("Registering client identity with TTP identityName={}", request.getIdentityName());
            RegistrationResponse response = restClient.post()
                    .uri(ttpServiceBaseUrl + "/api/ttp/register/user")
                    .body(request)
                    .retrieve()
                    .body(RegistrationResponse.class);
            if (response == null) {
                throw new TtpClientException("TTP returned an empty registration response");
            }
            return response;
        } catch (TtpClientException e) {
            throw e;
        } catch (Exception e) {
            throw new TtpClientException("Failed to register user with TTP", e);
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
}
