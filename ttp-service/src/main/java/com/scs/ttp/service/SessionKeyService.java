package com.scs.ttp.service;

import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.ttp.exception.TtpOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.PublicKey;

@Service
@RequiredArgsConstructor
public class SessionKeyService {

    private final AesKeyService aesKeyService;
    private final RsaEncryptionService rsaEncryptionService;
    private final EncodingService encodingService;

    public SecretKey generateSessionKey() {
        try {
            return aesKeyService.generateSessionKey();
        } catch (Exception e) {
            throw new TtpOperationException("Failed to generate AES session key", e);
        }
    }

    public String encryptSessionKeyForRecipient(SecretKey sessionKey, PublicKey recipientPublicKey) {
        try {
            byte[] encrypted = rsaEncryptionService.encrypt(sessionKey.getEncoded(), recipientPublicKey);
            return encodingService.encodeBase64(encrypted);
        } catch (Exception e) {
            throw new TtpOperationException("Failed to encrypt session key for recipient", e);
        }
    }
}
