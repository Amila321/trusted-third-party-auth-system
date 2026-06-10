package com.scs.crypto.aes;

import com.scs.crypto.config.CryptoConstants;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class AesEncryptionService {

    /**
     * Encrypts {@code plaintext} using AES-256-CBC with the given {@code iv}.
     * The caller must supply a fresh, random IV for each encryption — never reuse an IV with the same key.
     *
     * @throws GeneralSecurityException on cipher initialization or operation failure
     */
    public byte[] encrypt(byte[] plaintext, SecretKey secretKey, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptoConstants.AES_CIPHER_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(plaintext);
    }

    /**
     * Decrypts {@code ciphertext} using AES-256-CBC with the same {@code iv} used during encryption.
     *
     * @throws GeneralSecurityException on cipher initialization or operation failure
     */
    public byte[] decrypt(byte[] ciphertext, SecretKey secretKey, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptoConstants.AES_CIPHER_MODE);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    /**
     * Generates a cryptographically random 128-bit (16-byte) initialization vector for AES-CBC.
     */
    public byte[] generateIv() {
        byte[] iv = new byte[CryptoConstants.AES_IV_SIZE_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
