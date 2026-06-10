package com.scs.server.service;

import com.scs.crypto.aes.AesEncryptionService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
import com.scs.server.dto.SessionCloseResponse;
import com.scs.server.exception.DataExchangeException;
import com.scs.server.model.ServerSessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExchangeService {

    private final AesEncryptionService aesEncryptionService;
    private final EncodingService encodingService;
    private final InMemoryServerSessionStore sessionStore;

    public EncryptedDataResponse decryptAndProcess(EncryptedDataRequest request) {
        ServerSessionContext session = sessionStore.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new DataExchangeException("Session not found: " + request.getSessionId()));
        SecretKey sessionKey = session.getSessionKey();
        if (sessionKey == null) {
            throw new DataExchangeException("Session key is not available for session: " + request.getSessionId());
        }

        try {
            byte[] plaintextBytes = aesEncryptionService.decrypt(
                    encodingService.decodeBase64(request.getCiphertext()),
                    sessionKey,
                    encodingService.decodeBase64(request.getIv())
            );
            String plaintext = new String(plaintextBytes, StandardCharsets.UTF_8);
            log.info("Decrypted client payload sessionId={} userId={}", request.getSessionId(), session.getUserId());

            String processed = "server processed: " + plaintext;
            byte[] responseIv = aesEncryptionService.generateIv();
            byte[] responseCiphertext = aesEncryptionService.encrypt(
                    processed.getBytes(StandardCharsets.UTF_8),
                    sessionKey,
                    responseIv
            );

            return EncryptedDataResponse.builder()
                    .sessionId(request.getSessionId())
                    .ciphertext(encodingService.encodeBase64(responseCiphertext))
                    .iv(encodingService.encodeBase64(responseIv))
                    .build();
        } catch (DataExchangeException e) {
            throw e;
        } catch (Exception e) {
            throw new DataExchangeException("Failed to decrypt or process encrypted payload", e);
        }
    }

    public SessionCloseResponse closeSession(String sessionId) {
        sessionStore.close(sessionId);
        return SessionCloseResponse.builder()
                .sessionId(sessionId)
                .closed(true)
                .build();
    }
}
