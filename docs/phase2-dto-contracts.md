# Phase 2: Shared DTO Contracts

## Overview

All inter-service request/response objects are now defined in `dto-common`. Every service (`ttp-service`, `server-service`, `client-backend`) already depends on this module, so the contracts are immediately available across the project.

## Files Changed

### `dto-common/pom.xml`

Added dependencies:
- `jackson-annotations` — `@JsonProperty` for explicit JSON field names
- `jakarta.validation-api` — `@NotBlank` / `@NotNull` on request fields
- `lombok` (optional) — `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `spring-boot-starter-test` (test scope) — JUnit 5 + AssertJ for serialization tests

## Files Created

### Auth DTOs — `com.scs.dto.auth`

| Class | Direction | Key Fields |
|---|---|---|
| `RegistrationRequest` | Client/Server → TTP | `identity_name`, `public_key_pem` |
| `RegistrationResponse` | TTP → Client/Server | `identity_id`, `certificate_pem`, `registered_at` |
| `LoginRequest` | Client → TTP | `identity_id` |
| `LoginResponse` | TTP → Client | `identity_id`, `certificate_pem`, `challenge` |
| `UserAuthenticationRequest` | User → Server | `user_id`, `server_id`, `user_certificate_pem`, `challenge`, `signed_challenge` |
| `ServerAuthenticationRequest` | Server → TTP | all of the above + `server_certificate_pem` |
| `TtpAuthenticationDecision` | TTP → Server | `authenticated`, `session_id`, `encrypted_session_key_for_user`, `encrypted_session_key_for_server`, `rejection_reason`, `decided_at` |

### Session DTO — `com.scs.dto.session`

| Class | Key Fields |
|---|---|
| `SessionKeyResponse` | `session_id`, `encrypted_session_key` (Base64, RSA-wrapped AES-256), `issued_at` |

### Data Exchange DTOs — `com.scs.dto.data`

| Class | Key Fields |
|---|---|
| `EncryptedDataRequest` | `session_id`, `ciphertext` (Base64), `iv` (Base64) |
| `EncryptedDataResponse` | `session_id`, `ciphertext` (Base64), `iv` (Base64) |

### Common DTO — `com.scs.dto.common`

| Class | Key Fields |
|---|---|
| `ErrorResponse` | `error_code`, `message`, `details`, `timestamp` |

### Test — `com.scs.dto.DtoSerializationTest`

12 unit tests covering every DTO with JSON round-trips (serialize → deserialize → equality). Tests also assert that field names in JSON use `snake_case` as defined by `@JsonProperty`.

## Design Conventions

- All DTOs are pure data carriers — no business logic.
- JSON field names use `snake_case` via explicit `@JsonProperty`.
- `@NotBlank` is applied only to fields on inbound request DTOs (not responses).
- Timestamps are ISO 8601 UTC strings (e.g. `2026-06-09T12:00:00Z`).
- Cryptographic payloads (keys, ciphertext, IV) are Base64-encoded strings.
- Certificates and public keys use PEM format (`-----BEGIN ...-----`).
