package com.scs.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionKeyResponse {

    @JsonProperty("session_id")
    private String sessionId;

    /** Base64-encoded AES-256 key encrypted with recipient's RSA public key. */
    @JsonProperty("encrypted_session_key")
    private String encryptedSessionKey;

    @JsonProperty("issued_at")
    private String issuedAt;
}
