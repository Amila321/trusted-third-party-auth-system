# Phase 7: Encrypted Client-Server Data Exchange

## Overview

Phase 7 adds AES-256-CBC encrypted data exchange after authentication. The client-backend encrypts plaintext with the stored session key, sends the encrypted payload to server-service, receives an encrypted response, decrypts it, and returns both encrypted artifacts plus the decrypted server response for demo visibility.

## Client Backend Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/client/data/encrypt-and-send` | Encrypts plaintext, sends it to server-service, decrypts the encrypted server response |
| `POST` | `/api/client/session/close` | Closes the local client session and asks server-service to close the server session |

Request for `/api/client/data/encrypt-and-send`:

```json
{
  "session_id": "session-id",
  "plaintext": "message to protect"
}
```

## Server Service Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/server/data/decrypt-and-process` | Decrypts `EncryptedDataRequest`, processes plaintext, returns encrypted response |
| `POST` | `/api/server/session/close` | Removes the server session context |

Server data exchange uses shared `EncryptedDataRequest` / `EncryptedDataResponse` DTOs:

```json
{
  "session_id": "session-id",
  "ciphertext": "base64-aes-cbc-ciphertext",
  "iv": "base64-16-byte-iv"
}
```

## Security Behavior

- AES-256-CBC is used through `crypto-common`.
- A fresh random IV is generated for every client request encryption and every server response encryption.
- Both services require an existing session id.
- Server-side data exchange requires the decrypted AES `SecretKey` in `ServerSessionContext`.
- Invalid ciphertext, missing sessions, or sessions without a decrypted key are rejected.
- Session close removes in-memory session state.

## Tests Added

| Test Class | Coverage |
|---|---|
| `server-service/.../DataExchangeControllerTest` | Successful decrypt/process/encrypt, missing session, invalid ciphertext, session without decrypted key, close session |
| `client-backend/.../DataExchangeControllerTest` | Client encryption, server response decryption, missing client session, server unavailable, close session, validation failure |
| `client-backend/.../ServerConnectionTest` | Outbound encrypted data request, close-session request, server error handling |

## Verification

Run:

```bash
mvn -pl server-service,client-backend -am test
```

The command could not be executed in the Codex shell because Maven is not available on `PATH`.
