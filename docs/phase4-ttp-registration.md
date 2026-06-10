# Phase 4: TTP Registration & Certificate Issuance

## Overview

The TTP service now supports in-memory registration for User and Server identities and issues TTP-signed X.509 certificates for registered public keys.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/ttp/register/user` | Registers a user identity and returns a signed certificate |
| `POST` | `/api/ttp/register/server` | Registers a server identity and returns a signed certificate |
| `GET` | `/api/ttp/certificate/{identityId}` | Retrieves the registered certificate for an identity |

Both registration endpoints consume `RegistrationRequest` from `dto-common`:

```json
{
  "identity_name": "alice",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----..."
}
```

Successful responses use `RegistrationResponse`:

```json
{
  "identity_id": "sha256-derived-id",
  "certificate_pem": "-----BEGIN CERTIFICATE-----...",
  "registered_at": "2026-06-10T12:00:00Z"
}
```

## Files Added

| File | Purpose |
|---|---|
| `ttp-service/src/main/java/com/scs/ttp/model/IdentityType.java` | Distinguishes `USER` and `SERVER` registrations |
| `ttp-service/src/main/java/com/scs/ttp/model/RegisteredIdentity.java` | In-memory identity record |
| `ttp-service/src/main/java/com/scs/ttp/service/TtpCertificateAuthority.java` | Owns the TTP CA key pair and signs certificates |
| `ttp-service/src/main/java/com/scs/ttp/service/InMemoryIdentityStore.java` | Stores identities and prevents duplicate names |
| `ttp-service/src/main/java/com/scs/ttp/service/RegistrationService.java` | Coordinates public key parsing, certificate issuance, and response mapping |
| `ttp-service/src/main/java/com/scs/ttp/controller/RegistrationController.java` | Exposes Phase 4 registration and certificate endpoints |
| `ttp-service/src/main/java/com/scs/ttp/controller/TtpExceptionHandler.java` | Converts duplicate, missing identity, validation, and operation errors into `ErrorResponse` |
| `ttp-service/src/test/java/com/scs/ttp/controller/RegistrationControllerTest.java` | Endpoint tests for registration, duplicate rejection, and certificate retrieval |

## Design Notes

- TTP state is stored in memory only, aligned with the MVP database decision.
- Identity IDs are deterministic SHA-256 hashes of normalized identity names.
- Duplicate identity names are rejected with HTTP `409 CONFLICT`.
- Missing identities return HTTP `404 NOT_FOUND`.
- The TTP CA key pair is generated in memory on application startup.
- Certificates are issued through `crypto-common` and returned in PEM format.

## Verification Status

The intended verification command is:

```bash
mvn -pl ttp-service -am test
```

Verification could not be run in the current shell because Maven is not available on `PATH` and no Maven wrapper is present in the repository.
