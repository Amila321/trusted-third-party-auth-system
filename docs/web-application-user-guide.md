# Web Application User Guide

## Purpose

The web application is an educational interface for demonstrating the Trusted Third Party (TTP) authentication scenario. It is designed to make every important protocol step visible:

- service health checks
- user registration at the TTP
- RSA public key and X.509 certificate handling
- TTP session-key distribution
- AES encrypted client-server data exchange
- request and response payloads for each step
- errors returned by backend services

The application is not only a control panel. It is also a protocol trace viewer for explaining what happens inside the TTP project.

## Required Services

The frontend runs in the browser, but it depends on backend services.

Required for service status and user registration:

- `client-backend` on `http://localhost:8082`
- `ttp-service` on `http://localhost:8080`

Required for encrypted data exchange:

- `server-service` on `http://localhost:8081`

The frontend itself runs on:

```text
http://localhost:5173
```

or:

```text
http://127.0.0.1:5173
```

## Start The Application Locally

From the repository root, start the Java services in separate terminals.

Terminal 1:

```bash
mvn -pl ttp-service spring-boot:run
```

Terminal 2:

```bash
mvn -pl server-service spring-boot:run
```

Terminal 3:

```bash
mvn -pl client-backend spring-boot:run
```

Terminal 4:

```bash
cd client-frontend
npm install
npm run dev
```

If PowerShell blocks `npm.ps1`, use:

```powershell
npm.cmd run dev
```

Open the Vite URL printed in the terminal, normally:

```text
http://localhost:5173
```

## First Screen

The page is titled:

```text
SCS TTP Authentication Demo
```

The screen has four main areas:

1. Service status rail
2. Protocol timeline
3. Scenario panels
4. Event log

## Service Status Rail

The left side shows three service cards:

- Client Backend
- Server Service
- TTP Service

Each card shows:

- service name reported by the backend
- current status, usually `UP` or `DOWN`

Use the `Refresh` button to call:

```http
GET /api/client/status
```

The client backend then checks:

```http
GET http://localhost:8081/api/health
GET http://localhost:8080/api/health
```

If a service is down, the card will show that the affected service is unavailable.

## Protocol Timeline

The protocol timeline shows the educational flow:

1. Registration
2. TTP decision
3. Client completion
4. Encrypted data

The timeline changes state as the demo progresses.

Registration becomes complete after the user is registered at the TTP.

Authentication becomes complete after the client backend stores the AES session key.

Encrypted data becomes complete after the client sends encrypted data to the server and decrypts the server response.

## Step 1: Register User At TTP

Use the panel:

```text
Register User at TTP
```

Enter a user identity name, for example:

```text
alice
```

Click:

```text
Generate RSA keys and register
```

This calls:

```http
POST http://localhost:8082/api/client/auth/initiate
```

Request shape:

```json
{
  "identity_name": "alice"
}
```

What happens internally:

1. The client backend generates an RSA-4096 key pair.
2. The client backend keeps the private key in memory.
3. The client backend sends the public key to the TTP.
4. The TTP stores the identity in memory.
5. The TTP signs an X.509 certificate for the user.
6. The client backend stores the identity id, public key, private key, and certificate in memory.
7. The frontend displays the returned registration data.

Expected response fields:

```json
{
  "identity_id": "sha256-derived-user-id",
  "certificate_pem": "-----BEGIN CERTIFICATE-----...",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----...",
  "registered_at": "2026-06-10T..."
}
```

Important educational details:

- `public_key_pem` is safe to share.
- the private key is not shown in the browser.
- `certificate_pem` is the TTP-issued certificate proving the public key belongs to the registered identity.
- `identity_id` is the identifier used in later protocol steps.

## Step 2: TTP Authentication Decision

The current frontend does not directly create a complete User-to-Server signed challenge request by itself. This part is intentionally shown as a visible handoff.

Use the panel:

```text
Complete TTP Session
```

This panel expects values that come from a TTP authentication decision:

- `session_id`
- `encrypted_session_key_for_user`

These values are produced when the server forwards authentication material to the TTP.

The relevant backend endpoint on the TTP side is:

```http
POST http://localhost:8080/api/ttp/auth/user
```

The TTP decision shape is:

```json
{
  "authenticated": true,
  "session_id": "session-id",
  "encrypted_session_key_for_user": "base64-rsa-encrypted-aes-key",
  "encrypted_session_key_for_server": "base64-rsa-encrypted-aes-key",
  "rejection_reason": null,
  "decided_at": "2026-06-10T..."
}
```

For the frontend completion panel, copy:

```json
{
  "session_id": "session-id",
  "encrypted_session_key": "value from encrypted_session_key_for_user",
  "issued_at": "timestamp"
}
```

The frontend labels this as:

```text
Encrypted AES key for User
```

Paste the `encrypted_session_key_for_user` value there.

## Step 3: Complete Client Authentication

After filling the TTP session values, click:

```text
Decrypt and store AES session key
```

This calls:

```http
POST http://localhost:8082/api/client/auth/complete
```

Request shape:

```json
{
  "session_id": "session-id",
  "encrypted_session_key": "base64-rsa-encrypted-aes-key-for-user",
  "issued_at": "2026-06-10T..."
}
```

What happens internally:

1. The client backend finds the active registered identity.
2. It uses the private RSA key generated during registration.
3. It decrypts the TTP-provided encrypted AES session key.
4. It stores the AES-256 session key in memory.
5. It returns session status to the frontend.

Expected response:

```json
{
  "session_id": "session-id",
  "identity_id": "user-id",
  "session_key_base64": "base64-aes-key",
  "authenticated": true
}
```

For education and debugging, the response currently shows `session_key_base64`. In a production system this would not be returned to a browser.

## Step 4: Encrypted Data Exchange

Use the panel:

```text
Encrypted Data Exchange
```

Enter plaintext, for example:

```text
Message protected by AES-256-CBC
```

Click:

```text
Encrypt, send, decrypt response
```

This calls:

```http
POST http://localhost:8082/api/client/data/encrypt-and-send
```

Request shape:

```json
{
  "session_id": "session-id",
  "plaintext": "Message protected by AES-256-CBC"
}
```

What happens internally:

1. The client backend finds the AES session key for the session id.
2. The client backend generates a fresh AES-CBC IV.
3. The client backend encrypts the plaintext.
4. The client backend sends encrypted data to the server service.
5. The server service finds the same AES session key.
6. The server decrypts the ciphertext.
7. The server processes the plaintext.
8. The server encrypts its response with a fresh IV.
9. The client backend decrypts the server response.
10. The frontend displays both encrypted artifacts and decrypted response text.

The client-to-server encrypted request uses:

```json
{
  "session_id": "session-id",
  "ciphertext": "base64-ciphertext",
  "iv": "base64-iv"
}
```

The server response uses:

```json
{
  "session_id": "session-id",
  "ciphertext": "base64-ciphertext",
  "iv": "base64-iv"
}
```

The frontend displays:

- encrypted request
- encrypted response
- decrypted response

Expected decrypted response:

```text
server processed: Message protected by AES-256-CBC
```

## Close Session

Click:

```text
Close session
```

This calls:

```http
POST http://localhost:8082/api/client/session/close
```

Request shape:

```json
{
  "session_id": "session-id"
}
```

What happens internally:

1. The client backend asks server-service to close the server session.
2. The server removes the session from its in-memory store.
3. The client backend removes the local session from memory.
4. The frontend clears the active session state.

## Event Log

The event log is at the bottom of the page.

It records:

- service status refreshes
- registration start and result
- authentication completion start and result
- encrypted data request and result
- session close operations
- errors returned by backend services

Each log entry includes:

- time
- event title
- human-readable explanation
- JSON payload if available

Use `Clear` to empty the log.

## Common Errors

### Error: `404 Not Found` for `/api/client/auth/initiate`

Example:

```json
{
  "status": 404,
  "error": "Not Found",
  "path": "/api/client/auth/initiate"
}
```

Meaning:

The currently running `client-backend` process is stale and does not include the Phase 6 authentication controller.

Fix:

Stop and restart `client-backend`.

```bash
mvn -pl client-backend -am spring-boot:run
```

If using IntelliJ, stop the old run configuration and start it again.

If using Docker, rebuild the image before starting containers.

### Error: `TTP_UNAVAILABLE`

Meaning:

The client backend tried to call the TTP service, but the TTP service was unreachable or returned an error.

Check:

```bash
curl http://localhost:8080/api/health
```

Expected:

```json
{
  "service": "ttp-service",
  "status": "UP"
}
```

### Error: `SERVER_UNAVAILABLE`

Meaning:

The client backend tried to call server-service during encrypted data exchange, but server-service was unreachable or returned an error.

Check:

```bash
curl http://localhost:8081/api/health
```

Expected:

```json
{
  "service": "server-service",
  "status": "UP"
}
```

### Error: `DATA_EXCHANGE_FAILED`

Possible causes:

- no session exists for the provided `session_id`
- the AES session key was not stored
- ciphertext or IV is invalid
- the server does not have a decrypted session key

Important:

For server-side encrypted data exchange, server-service must have access to the AES session key. In the implemented backend, this is possible when the server can decrypt `encrypted_session_key_for_server` using its configured RSA private key.

Configuration keys:

```yaml
server:
  identity:
    certificate-pem: "..."
    private-key-pem: "..."
```

### Error: Duplicate Registration

If the same identity name is registered twice during one TTP runtime, TTP rejects it.

Reason:

The TTP uses in-memory identity storage and prevents duplicate names.

Fix:

Use a different identity name, for example:

```text
alice-2
```

or restart the TTP service to clear in-memory state.

## Recommended Demo Script

1. Start `ttp-service`, `server-service`, `client-backend`, and `client-frontend`.
2. Open `http://localhost:5173`.
3. Click `Refresh` in the service status rail.
4. Confirm all services are `UP`.
5. Register a user named `alice`.
6. Show the generated `identity_id`, `public_key_pem`, and `certificate_pem`.
7. Explain that the private RSA key remains in client-backend memory.
8. Obtain or provide a TTP authentication decision containing `session_id` and `encrypted_session_key_for_user`.
9. Paste those values into `Complete TTP Session`.
10. Click `Decrypt and store AES session key`.
11. Show the active session badge.
12. Enter plaintext in `Encrypted Data Exchange`.
13. Click `Encrypt, send, decrypt response`.
14. Show encrypted request JSON and encrypted response JSON.
15. Show decrypted server response.
16. Click `Close session`.
17. Show the event log as a complete protocol trace.

## Educational Notes

The frontend intentionally exposes protocol artifacts that would normally be hidden in a production application.

Visible for education:

- public key PEM
- certificate PEM
- encrypted session keys
- encrypted payloads
- IVs
- decrypted server response
- event log payloads

Should not be exposed in production:

- AES session keys
- private keys
- internal authentication state
- full cryptographic debug logs

This project is an MVP demonstration of the TTP authentication scenario, so visibility is prioritized to help explain the protocol.
