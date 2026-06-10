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
public class ClientSessionStatusResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("identity_id")
    private String identityId;

    @JsonProperty("session_key_base64")
    private String sessionKeyBase64;

    @JsonProperty("authenticated")
    private boolean authenticated;
}
