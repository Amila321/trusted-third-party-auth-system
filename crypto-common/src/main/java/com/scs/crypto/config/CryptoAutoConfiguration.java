package com.scs.crypto.config;

import com.scs.crypto.aes.AesEncryptionService;
import com.scs.crypto.aes.AesKeyService;
import com.scs.crypto.certificate.CertificateService;
import com.scs.crypto.encoding.EncodingService;
import com.scs.crypto.hash.HashService;
import com.scs.crypto.rsa.RsaEncryptionService;
import com.scs.crypto.rsa.RsaKeyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoAutoConfiguration {

    @Bean
    public EncodingService encodingService() {
        return new EncodingService();
    }

    @Bean
    public RsaKeyService rsaKeyService() {
        return new RsaKeyService();
    }

    @Bean
    public RsaEncryptionService rsaEncryptionService() {
        return new RsaEncryptionService();
    }

    @Bean
    public AesKeyService aesKeyService() {
        return new AesKeyService();
    }

    @Bean
    public AesEncryptionService aesEncryptionService() {
        return new AesEncryptionService();
    }

    @Bean
    public HashService hashService() {
        return new HashService();
    }

    @Bean
    public CertificateService certificateService() {
        return new CertificateService();
    }
}
