package com.scs.crypto.config;

public final class CryptoConstants {

    private CryptoConstants() {}

    public static final int RSA_KEY_SIZE = 4096;
    public static final int AES_KEY_SIZE = 256;
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_CIPHER_MODE = "AES/CBC/PKCS5Padding";
    public static final int AES_IV_SIZE_BYTES = 16;
    public static final String RSA_ALGORITHM = "RSA";
    public static final String RSA_CIPHER_MODE = "RSA/ECB/PKCS1Padding";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int CERTIFICATE_VALIDITY_DAYS = 365;
    public static final String X509_VERSION = "X.509";
    public static final String BOUNCY_CASTLE_PROVIDER = "BC";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final String TTP_ISSUER_DN = "CN=SCS-TTP,O=SCS,C=PL";
}
