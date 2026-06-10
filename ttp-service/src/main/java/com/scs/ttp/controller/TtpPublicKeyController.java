package com.scs.ttp.controller;

import com.scs.crypto.rsa.RsaKeyService;
import com.scs.dto.auth.TtpPublicKeyResponse;
import com.scs.ttp.exception.TtpOperationException;
import com.scs.ttp.service.TtpCertificateAuthority;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ttp")
public class TtpPublicKeyController {

    private final TtpCertificateAuthority certificateAuthority;
    private final RsaKeyService rsaKeyService;

    @GetMapping("/public-key")
    public TtpPublicKeyResponse publicKey() {
        try {
            return TtpPublicKeyResponse.builder()
                    .publicKeyPem(rsaKeyService.encodePublicKeyPem(certificateAuthority.getTtpKeyPair().getPublic()))
                    .issuedAt(Instant.now().toString())
                    .build();
        } catch (Exception e) {
            throw new TtpOperationException("Failed to encode TTP public key", e);
        }
    }
}
