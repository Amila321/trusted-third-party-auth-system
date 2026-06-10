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
public class LoginResponse {

    @JsonProperty("identity_id")
    private String identityId;

    @JsonProperty("certificate_pem")
    private String certificatePem;

    @JsonProperty("challenge")
    private String challenge;
}
