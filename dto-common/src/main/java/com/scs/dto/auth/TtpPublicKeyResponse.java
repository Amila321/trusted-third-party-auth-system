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
public class TtpPublicKeyResponse {

    @JsonProperty("public_key_pem")
    private String publicKeyPem;

    @JsonProperty("issued_at")
    private String issuedAt;
}
