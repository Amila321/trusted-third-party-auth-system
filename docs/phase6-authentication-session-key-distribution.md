# Phase 6: TTP Authentication & Session Key Distribution

## Overview

Phase 6 completes the authentication decision path between Server and TTP and adds client-backend support for initiating a user identity and decrypting the user-wrapped AES session key.

## TTP Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/ttp/auth/user` | Validates User-to-Server authentication material and returns a session decision |
| `POST` | `/api/ttp/auth/server` | Alias for the same validation flow |
| `POST` | `/api/ttp/auth/validate` | Alias for the same validation flow |

Input uses `ServerAuthenticationRequest`:

```json
{
  "user_id": "registered-user-id",
  "server_id": "registered-server-id",
  "user_certificate_pem": "-----BEGIN CERTIFICATE-----...",
  "server_certificate_pem": "-----BEGIN CERTIFICATE-----...",
  "challenge": "challenge",
  "signed_challenge": "base64-sha256-with-rsa-signature"
}
```

Successful authentication returns `TtpAuthenticationDecision` with one AES-256 session key encrypted separately for User and Server.

## Client Backend Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/client/auth/initiate` | Generates a client RSA key pair, registers the user at TTP, and stores identity material in memory |
| `POST` | `/api/client/auth/complete` | Decrypts a `SessionKeyResponse` encrypted for the active client identity and stores the AES session key |

## Security Behavior

- TTP validates that both User and Server identities are registered.
- Presented certificates must be signed by the TTP CA, be currently valid, and exactly match the certificate stored during registration.
- User challenge signatures are verified with `SHA256withRSA` over raw UTF-8 challenge bytes.
- Session keys are AES-256 keys generated in TTP memory.
- Session keys are RSA-encrypted separately for User and Server and returned as Base64 strings.
- TTP and client session state remains in memory for the MVP.

## Tests Added

| Test Class | Coverage |
|---|---|
| `ttp-service/.../AuthenticationControllerTest` | Valid auth flow, RSA-unwrapped matching AES keys, session storage, invalid signature rejection, unknown identity rejection, certificate mismatch rejection |
| `client-backend/.../AuthenticationControllerTest` | Client initiate, TTP registration payload, client completion/decryption, TTP registration failure, validation failure, completion without usable identity |

## Verification

Run:

```bash
mvn -pl ttp-service,client-backend -am test
```

The command could not be executed in the Codex shell because Maven is not available on `PATH`.
