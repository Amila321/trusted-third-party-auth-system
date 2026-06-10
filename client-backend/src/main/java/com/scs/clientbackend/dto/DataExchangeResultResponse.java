package com.scs.clientbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExchangeResultResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("encrypted_request")
    private EncryptedDataRequest encryptedRequest;

    @JsonProperty("encrypted_response")
    private EncryptedDataResponse encryptedResponse;

    @JsonProperty("decrypted_response")
    private String decryptedResponse;
}
