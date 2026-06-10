# Phase 3: Cryptographic Utilities

## Overview

All cryptographic operations are centralized in `crypto-common`. Every consuming service (`ttp-service`, `server-service`, `client-backend`) already depends on this module. The services auto-register into any Spring Boot application context through Spring Boot's autoconfiguration mechanism — no `@ComponentScan` changes are required.

---

## Files Changed

### `crypto-common/pom.xml`

Added dependencies:
- `spring-context` (optional) — provides `@Configuration` / `@Bean` for the auto-configuration class
- `lombok` (optional) — available for `@Slf4j` in future phases
- `spring-boot-starter-test` (test scope) — JUnit 5 + AssertJ

---

## Files Created

### `config/CryptoConstants.java`

Single source of truth for all algorithm names and sizes:

| Constant | Value |
|---|---|
| `RSA_KEY_SIZE` | `4096` |
| `AES_KEY_SIZE` | `256` |
| `AES_CIPHER_MODE` | `AES/CBC/PKCS5Padding` |
| `AES_IV_SIZE_BYTES` | `16` |
| `RSA_CIPHER_MODE` | `RSA/ECB/PKCS1Padding` |
| `HASH_ALGORITHM` | `SHA-256` |
| `CERTIFICATE_VALIDITY_DAYS` | `365` |
| `SIGNATURE_ALGORITHM` | `SHA256WithRSA` |
| `TTP_ISSUER_DN` | `CN=SCS-TTP,O=SCS,C=PL` |

The static initializer calls `Security.addProvider(new BouncyCastleProvider())` so BC is registered once on first class load.

### `config/CryptoAutoConfiguration.java`

A `@Configuration` class that declares a `@Bean` for each service class. Registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` so that all beans are available in any Spring Boot application that depends on `crypto-common`, without any additional scanning configuration.

### `encoding/EncodingService.java`

| Method | Description |
|---|---|
| `encodeBase64(byte[])` | Standard Base64 encode |
| `decodeBase64(String)` | Standard Base64 decode |

### `rsa/RsaKeyService.java`

| Method | Description |
|---|---|
| `generateKeyPair()` | RSA-4096 via `KeyPairGenerator` + `SecureRandom` |
| `encodePublicKeyPem(PublicKey)` | PKCS#8 SubjectPublicKeyInfo → `BEGIN PUBLIC KEY` PEM |
| `decodePublicKeyPem(String)` | PEM → `PublicKey` via BC `PEMParser` + `JcaPEMKeyConverter` |
| `encodePrivateKeyPem(PrivateKey)` | PKCS#8 → `BEGIN PRIVATE KEY` PEM |
| `decodePrivateKeyPem(String)` | Handles both PKCS#8 (`PrivateKeyInfo`) and PKCS#1 (`PEMKeyPair`) formats |

### `rsa/RsaEncryptionService.java`

| Method | Description |
|---|---|
| `encrypt(byte[], PublicKey)` | RSA/ECB/PKCS1Padding via BC provider; suitable for ≤ 501-byte payloads (AES keys) |
| `decrypt(byte[], PrivateKey)` | Corresponding decryption |

### `aes/AesKeyService.java`

| Method | Description |
|---|---|
| `generateSessionKey()` | AES-256 via `KeyGenerator` + `SecureRandom` |
| `encodeKey(SecretKey)` | Base64-encode raw key bytes |
| `decodeKey(String)` | Reconstruct `SecretKey` from Base64 via `SecretKeySpec` |

### `aes/AesEncryptionService.java`

| Method | Description |
|---|---|
| `encrypt(byte[], SecretKey, byte[])` | AES-256-CBC with PKCS5 padding; **caller must supply a fresh IV per encryption** |
| `decrypt(byte[], SecretKey, byte[])` | Corresponding decryption with the same IV |
| `generateIv()` | 16 random bytes from `SecureRandom` |

### `hash/HashService.java`

| Method | Description |
|---|---|
| `hashIdentity(String)` | SHA-256 hex digest; derives opaque identity ID from a name |
| `hashChallenge(String, String)` | SHA-256 with salt prefix; used for challenge verification |

Both methods return lowercase hex strings (64 characters for SHA-256).

### `certificate/CertificateService.java`

| Method | Description |
|---|---|
| `generateCertificate(PublicKey, PrivateKey, String, int)` | Issues X.509 v3 cert via BC `JcaX509v3CertificateBuilder` + `JcaContentSignerBuilder`; signed with `SHA256WithRSA` |
| `encodeCertificatePem(X509Certificate)` | DER bytes → `BEGIN CERTIFICATE` PEM |
| `decodeCertificatePem(String)` | PEM → `X509Certificate` via BC `PEMParser` + `JcaX509CertificateConverter` |
| `validateCertificate(X509Certificate, PublicKey)` | Calls `certificate.verify(caTrustAnchor)`; returns `false` on any exception |
| `isCertificateExpired(X509Certificate)` | Calls `checkValidity()`; returns `true` if expired or not yet valid |
| `extractPublicKeyFromCertificate(X509Certificate)` | Returns embedded public key |
| `extractSubjectDN(X509Certificate)` | Returns subject principal name string |

---

## Resource Added

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

Contains one entry: `com.scs.crypto.config.CryptoAutoConfiguration`

Spring Boot 3.x reads this file at startup to auto-register all crypto beans without any component scanning in the consuming service.

---

## Tests Created

All tests are plain JUnit 5 (no Spring context). Services are instantiated with `new`.

| Test Class | Tests | Notes |
|---|---|---|
| `EncodingServiceTest` | 6 | Round-trip, known encoding, empty input, binary data |
| `HashServiceTest` | 7 | Consistency, distinctness, hex format validation |
| `AesKeyServiceTest` | 5 | Key size (256 bits), algorithm, round-trip encoding, distinctness |
| `AesEncryptionServiceTest` | 5 | Round-trip, IV randomness, different IVs → different ciphertext |
| `RsaKeyServiceTest` | 7 | Key size (4096 bits), PEM headers, round-trip encode/decode |
| `RsaEncryptionServiceTest` | 4 | Round-trip, AES key payload, non-deterministic output |
| `CertificateServiceTest` | 9 | Generation, subject DN, key match, PEM round-trip, CA validation, wrong-anchor rejection, expiry check |

**Performance note**: `RsaKeyServiceTest`, `RsaEncryptionServiceTest`, and `CertificateServiceTest` each use `@BeforeAll` to generate RSA-4096 key pairs once per class, keeping the total key-generation overhead to 3–4 pairs rather than one per test.

---

## Design Notes

- All service classes are **stateless and thread-safe** — no mutable instance state.
- Bouncy Castle is used for: PEM I/O, X.509 certificate generation, RSA operations (explicit `"BC"` provider). JDK provider is used for AES (JDK AES is well-tested and avoids a redundant BC dependency in the cipher path).
- The `TTP_ISSUER_DN` (`CN=SCS-TTP,O=SCS,C=PL`) is used as the certificate issuer in all TTP-issued certificates, making the issuer identity explicit and consistent.
- Private key PEM encoding uses PKCS#8 (`BEGIN PRIVATE KEY`) for portability; decoding handles both PKCS#8 and PKCS#1 RSA formats.
