package com.scs.crypto.encoding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncodingServiceTest {

    private EncodingService service;

    @BeforeEach
    void setUp() {
        service = new EncodingService();
    }

    @Test
    void encodeBase64_decodesToOriginalBytes() {
        byte[] data = "hello world".getBytes();
        String encoded = service.encodeBase64(data);
        byte[] decoded = service.decodeBase64(encoded);
        assertThat(decoded).isEqualTo(data);
    }

    @Test
    void encodeBase64_producesKnownEncoding() {
        // "Hello" in ASCII = {72, 101, 108, 108, 111}
        byte[] data = new byte[]{72, 101, 108, 108, 111};
        assertThat(service.encodeBase64(data)).isEqualTo("SGVsbG8=");
    }

    @Test
    void decodeBase64_knownInput() {
        byte[] expected = new byte[]{72, 101, 108, 108, 111};
        assertThat(service.decodeBase64("SGVsbG8=")).isEqualTo(expected);
    }

    @Test
    void encodeBase64_emptyArray_producesEmptyString() {
        assertThat(service.encodeBase64(new byte[0])).isEmpty();
    }

    @Test
    void decodeBase64_emptyString_producesEmptyArray() {
        assertThat(service.decodeBase64("")).isEmpty();
    }

    @Test
    void encode_decode_binaryData() {
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) binaryData[i] = (byte) i;
        assertThat(service.decodeBase64(service.encodeBase64(binaryData))).isEqualTo(binaryData);
    }
}
