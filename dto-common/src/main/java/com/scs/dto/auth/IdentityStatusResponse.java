package com.scs.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityStatusResponse {

    @JsonProperty("identity_id")
    private String identityId;

    @JsonProperty("identity_name")
    private String identityName;

    @JsonProperty("certificate_pem")
    private String certificatePem;

    @JsonProperty("public_key_pem")
    private String publicKeyPem;

    @JsonProperty("registered_at")
    private String registeredAt;

    @JsonProperty("registered")
    private boolean registered;
}
