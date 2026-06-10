package com.scs.clientbackend.service;

import com.scs.clientbackend.dto.DataExchangeResultResponse;
import com.scs.clientbackend.dto.PlaintextDataRequest;
import com.scs.clientbackend.dto.SessionCloseResponse;
import com.scs.clientbackend.exception.DataExchangeException;
import com.scs.clientbackend.exception.ServerConnectionException;
import com.scs.clientbackend.model.ClientSessionContext;
import com.scs.crypto.aes.AesEncryptionService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExchangeService {

    private final AesEncryptionService aesEncryptionService;
    private final EncodingService encodingService;
    private final InMemoryClientAuthenticationStore authenticationStore;
    private final ServerConnection serverConnection;

    public DataExchangeResultResponse encryptAndSend(PlaintextDataRequest request) {
        ClientSessionContext session = authenticationStore.findSession(request.getSessionId())
                .orElseThrow(() -> new DataExchangeException("Session not found: " + request.getSessionId()));

        try {
            byte[] requestIv = aesEncryptionService.generateIv();
            byte[] requestCiphertext = aesEncryptionService.encrypt(
                    request.getPlaintext().getBytes(StandardCharsets.UTF_8),
                    session.getSessionKey(),
                    requestIv
            );
            EncryptedDataRequest encryptedRequest = EncryptedDataRequest.builder()
                    .sessionId(request.getSessionId())
                    .ciphertext(encodingService.encodeBase64(requestCiphertext))
                    .iv(encodingService.encodeBase64(requestIv))
                    .build();

            EncryptedDataResponse encryptedResponse = serverConnection.sendEncryptedData(encryptedRequest);
            byte[] responsePlaintext = aesEncryptionService.decrypt(
                    encodingService.decodeBase64(encryptedResponse.getCiphertext()),
                    session.getSessionKey(),
                    encodingService.decodeBase64(encryptedResponse.getIv())
            );
            String decryptedResponse = new String(responsePlaintext, StandardCharsets.UTF_8);
            log.info("Completed encrypted data exchange sessionId={}", request.getSessionId());

            return DataExchangeResultResponse.builder()
                    .sessionId(request.getSessionId())
                    .encryptedRequest(encryptedRequest)
                    .encryptedResponse(encryptedResponse)
                    .decryptedResponse(decryptedResponse)
                    .build();
        } catch (DataExchangeException e) {
            throw e;
        } catch (ServerConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new DataExchangeException("Failed to complete encrypted data exchange", e);
        }
    }

    public SessionCloseResponse closeSession(String sessionId) {
        serverConnection.closeServerSession(sessionId);
        authenticationStore.closeSession(sessionId);
        return SessionCloseResponse.builder()
                .sessionId(sessionId)
                .closed(true)
                .build();
    }
}
