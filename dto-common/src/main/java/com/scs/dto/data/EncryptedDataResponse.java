package com.scs.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedDataResponse {

    @JsonProperty("session_id")
    private String sessionId;

    /** Base64-encoded AES-256-CBC ciphertext. */
    @JsonProperty("ciphertext")
    private String ciphertext;

    /** Base64-encoded initialization vector. */
    @JsonProperty("iv")
    private String iv;
}
