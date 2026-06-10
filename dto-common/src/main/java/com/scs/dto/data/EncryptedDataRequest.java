package com.scs.dto.data;

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
public class EncryptedDataRequest {

    @JsonProperty("session_id")
    @NotBlank
    private String sessionId;

    /** Base64-encoded AES-256-CBC ciphertext. */
    @JsonProperty("ciphertext")
    @NotBlank
    private String ciphertext;

    /** Base64-encoded initialization vector. */
    @JsonProperty("iv")
    @NotBlank
    private String iv;
}
