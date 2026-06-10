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
public class TtpAuthenticationDecision {

    @JsonProperty("authenticated")
    private boolean authenticated;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("encrypted_session_key_for_user")
    private String encryptedSessionKeyForUser;

    @JsonProperty("encrypted_session_key_for_server")
    private String encryptedSessionKeyForServer;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("decided_at")
    private String decidedAt;
}
