package com.scs.clientbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAuthenticationStateResponse {

    @JsonProperty("identity_id")
    private String identityId;

    @JsonProperty("certificate_pem")
    private String certificatePem;

    @JsonProperty("public_key_pem")
    private String publicKeyPem;

    @JsonProperty("registered_at")
    private String registeredAt;
}
