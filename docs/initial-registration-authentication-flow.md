# Initial Registration & Authentication Preparation Flow

## Overview

The project now supports a complete demo flow before User and Server exchange encrypted data:

1. User registers with TTP.
2. Server registers with TTP.
3. Both parties derive identity IDs with SHA-256 over the normalized identity name.
4. Both parties encrypt their identity ID with the TTP public key.
5. Both parties send encrypted identity ID and RSA-4096 public key to TTP.
6. TTP decrypts and validates the identity ID, stores runtime identity state, and issues X.509 certificates.
7. User requests a session through Server.
8. Server forwards certificates and signed challenge to TTP.
9. TTP validates both certificates and distributes one AES-256 session key encrypted separately for User and Server.
10. Client backend stores the user AES key; server-service stores the server AES key.
11. Encrypted AES data exchange can start.

All runtime state remains in memory. No database is used.

## New And Updated Endpoints

### TTP Service

```http
GET /api/ttp/public-key
```

Returns the TTP public key PEM. The TTP private key is never exposed.

```http
POST /api/ttp/register/user
POST /api/ttp/register/server
```

Registration now requires:

```json
{
  "identity_name": "alice",
  "encrypted_identity_id": "base64-rsa-encrypted-sha256-id",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----..."
}
```

TTP decrypts `encrypted_identity_id` with its private key and validates it against `SHA-256(normalized identity_name)`.

### Server Service

```http
POST /api/server/auth/register
GET /api/server/auth/identity
POST /api/server/request
POST /api/server/data/decrypt-and-process
POST /api/server/session/close
```

Server registration generates and stores the Server RSA key pair only in `server-service` memory.

### Client Backend

```http
POST /api/client/auth/initiate
POST /api/client/server/register
GET /api/client/server/identity
POST /api/client/auth/request-session
POST /api/client/auth/request-and-complete-session
POST /api/client/auth/complete
POST /api/client/data/encrypt-and-send
POST /api/client/session/close
```

The frontend uses these endpoints to drive the full demo without manually guessing the `session_id`.

## Frontend Demo Flow

1. Start all services.
2. Open the frontend.
3. Refresh service status and confirm all services are `UP`.
4. Click `Generate RSA keys and register User`.
5. Click `Generate server RSA keys and register`.
6. Click `Request service/session`.
7. Inspect the full TTP decision JSON with `session_id`, `encrypted_session_key_for_user`, and `encrypted_session_key_for_server`.
8. Click `Fill from TTP decision`.
9. Click `Decrypt and store AES session key`.
10. Send plaintext through encrypted data exchange.
11. Inspect encrypted request, encrypted response, and decrypted response.
12. Close the session.

The optional `Request and complete session` button performs steps 6-9 in one backend call.

## Runtime Key Ownership

- User private key: stored only in `client-backend` memory.
- Server private key: stored only in `server-service` memory.
- TTP private key: stored only in `ttp-service` memory.
- Frontend never receives private keys.

## Verification Commands

Java:

```bash
mvn clean test
```

Frontend:

```bash
cd client-frontend
npm install
npm run build
```

In this Codex environment, Maven was not available on `PATH`, so Java tests could not be executed here. Frontend lint and build were executed with `npm.cmd` successfully.
