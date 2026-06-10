package com.scs.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCloseResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("closed")
    private boolean closed;
}
