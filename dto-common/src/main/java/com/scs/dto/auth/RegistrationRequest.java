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
public class RegistrationRequest {

    @JsonProperty("identity_name")
    @NotBlank
    private String identityName;

    @JsonProperty("public_key_pem")
    @NotBlank
    private String publicKeyPem;
}
