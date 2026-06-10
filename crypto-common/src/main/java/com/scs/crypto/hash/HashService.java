package com.scs.crypto.hash;

import com.scs.crypto.config.CryptoConstants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashService {

    /**
     * Returns the SHA-256 hex digest of the given identity name.
     * Used to derive a stable, opaque identity ID from a human-readable name.
     */
    public String hashIdentity(String identityName) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM);
        byte[] hash = digest.digest(identityName.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * Returns the SHA-256 hex digest of {@code challenge} prefixed with {@code salt}.
     * Used when verifying signed challenges to prevent replay attacks.
     */
    public String hashChallenge(String challenge, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(CryptoConstants.HASH_ALGORITHM);
        digest.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hash = digest.digest(challenge.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
