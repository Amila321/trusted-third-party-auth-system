package com.scs.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerAuthenticationRequest {

    @JsonProperty("user_id")
    @NotBlank
    private String userId;

    @JsonProperty("server_id")
    @NotBlank
    private String serverId;

    @JsonProperty("user_certificate_pem")
    @NotBlank
    private String userCertificatePem;

    @JsonProperty("server_certificate_pem")
    @NotBlank
    private String serverCertificatePem;

    @JsonProperty("challenge")
    @NotBlank
    private String challenge;

    @JsonProperty("signed_challenge")
    @NotBlank
    private String signedChallenge;
}
