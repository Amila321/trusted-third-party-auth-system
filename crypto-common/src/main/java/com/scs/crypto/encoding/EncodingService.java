package com.scs.crypto.encoding;

import java.util.Base64;

public class EncodingService {

    public String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public byte[] decodeBase64(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }
}
