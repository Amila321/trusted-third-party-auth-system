package com.scs.clientbackend.service;

import com.scs.clientbackend.dto.ServiceHealthResponse;
import com.scs.clientbackend.dto.SystemStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SystemStatusService {

    private final RestClient restClient;

    @Value("${services.server-service.base-url}")
    private String serverServiceBaseUrl;

    @Value("${services.ttp-service.base-url}")
    private String ttpServiceBaseUrl;

    public SystemStatusService() {
        this.restClient = RestClient.builder().build();
    }

    public SystemStatusResponse getSystemStatus() {
        ServiceHealthResponse clientStatus = new ServiceHealthResponse("client-backend", "UP");
        ServiceHealthResponse serverStatus = fetchHealth(serverServiceBaseUrl, "server-service");
        ServiceHealthResponse ttpStatus = fetchHealth(ttpServiceBaseUrl, "ttp-service");

        return new SystemStatusResponse(clientStatus, serverStatus, ttpStatus);
    }

    private ServiceHealthResponse fetchHealth(String baseUrl, String fallbackServiceName) {
        try {
            ServiceHealthResponse response = restClient.get()
                    .uri(baseUrl + "/api/health")
                    .retrieve()
                    .body(ServiceHealthResponse.class);

            if (response == null) {
                return new ServiceHealthResponse(fallbackServiceName, "DOWN");
            }

            return response;
        } catch (Exception exception) {
            return new ServiceHealthResponse(fallbackServiceName, "DOWN");
        }
    }
}