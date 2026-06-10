package com.scs.crypto.aes;

import com.scs.crypto.config.CryptoConstants;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AesKeyService {

    /**
     * Generates a cryptographically random AES-256 session key.
     *
     * @throws NoSuchAlgorithmException if AES is not available (should never occur on standard JVMs)
     */
    public SecretKey generateSessionKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(CryptoConstants.AES_ALGORITHM);
        keyGen.init(CryptoConstants.AES_KEY_SIZE, new SecureRandom());
        return keyGen.generateKey();
    }

    /**
     * Base64-encodes a {@link SecretKey} for transport or storage.
     */
    public String encodeKey(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * Reconstructs an AES {@link SecretKey} from its Base64-encoded byte representation.
     */
    public SecretKey decodeKey(String encodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(keyBytes, CryptoConstants.AES_ALGORITHM);
    }
}
