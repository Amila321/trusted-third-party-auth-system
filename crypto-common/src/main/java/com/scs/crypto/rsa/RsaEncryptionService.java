package com.scs.crypto.rsa;

import com.scs.crypto.config.CryptoConstants;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

public class RsaEncryptionService {

    // Same reason as CertificateService: static final String constants are inlined by javac,
    // so CryptoConstants' static block never runs via field access — register BC here instead.
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * Encrypts {@code plaintext} with RSA/ECB/PKCS1Padding using the recipient's public key.
     * Suitable for encrypting small payloads such as AES-256 session keys (≤ 501 bytes for RSA-4096).
     *
     * @throws GeneralSecurityException on cipher initialization or operation failure
     */
    public byte[] encrypt(byte[] plaintext, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptoConstants.RSA_CIPHER_MODE, CryptoConstants.BOUNCY_CASTLE_PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plaintext);
    }

    /**
     * Decrypts {@code ciphertext} with RSA/ECB/PKCS1Padding using the recipient's private key.
     *
     * @throws GeneralSecurityException on cipher initialization or operation failure
     */
    public byte[] decrypt(byte[] ciphertext, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CryptoConstants.RSA_CIPHER_MODE, CryptoConstants.BOUNCY_CASTLE_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(ciphertext);
    }
}
