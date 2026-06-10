# Phase 5: Server-Side Service Request Handling

## Overview

The Server service now accepts User service requests, enriches them with the configured Server certificate, forwards authentication material to the TTP, and stores accepted session decisions in memory.

## Endpoint

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/server/request` | Accepts a `UserAuthenticationRequest`, forwards it to TTP, and returns `TtpAuthenticationDecision` |

Input uses `UserAuthenticationRequest` from `dto-common`:

```json
{
  "user_id": "user-id",
  "server_id": "server-id",
  "user_certificate_pem": "-----BEGIN CERTIFICATE-----...",
  "challenge": "challenge",
  "signed_challenge": "base64-signature"
}
```

The server forwards a `ServerAuthenticationRequest` to the configured TTP endpoint after adding:

```json
{
  "server_certificate_pem": "configured-server-certificate"
}
```

## Configuration

`server-service/src/main/resources/application.yml`:

```yaml
services:
  ttp-service:
    base-url: http://localhost:8080
    auth-user-path: /api/ttp/auth/user

server:
  identity:
    certificate-pem: ""
```

The Docker profile uses `http://ttp-service:8080` for service-name networking.

## Files Added

| File | Purpose |
|---|---|
| `server-service/src/main/java/com/scs/server/config/RestClientConfig.java` | Provides a shared `RestClient` bean |
| `server-service/src/main/java/com/scs/server/service/TtpClient.java` | Calls the TTP authentication endpoint |
| `server-service/src/main/java/com/scs/server/service/ServerAuthenticationService.java` | Builds forwarded requests and stores accepted sessions |
| `server-service/src/main/java/com/scs/server/service/InMemoryServerSessionStore.java` | Temporary in-memory session decision storage |
| `server-service/src/main/java/com/scs/server/model/ServerSessionContext.java` | Stored server-side session context |
| `server-service/src/main/java/com/scs/server/controller/ServiceRequestController.java` | Exposes `POST /api/server/request` |
| `server-service/src/main/java/com/scs/server/controller/ServerExceptionHandler.java` | Maps validation, TTP, config, and invalid decision failures |

## Error Handling

| Condition | HTTP Status | `error_code` |
|---|---:|---|
| Invalid request DTO | `400` | `VALIDATION_ERROR` |
| Missing server certificate config | `500` | `SERVER_CONFIGURATION_ERROR` |
| TTP unavailable or invalid HTTP response | `503` | `TTP_UNAVAILABLE` |
| Authenticated TTP decision missing required session fields | `502` | `INVALID_TTP_DECISION` |

## Tests Added

| Test Class | Coverage |
|---|---|
| `ServiceRequestControllerTest` | Successful forwarding, accepted session storage, rejected decisions, TTP unavailable, invalid request DTO, malformed accepted TTP decision |
| `ServiceRequestControllerConfigurationTest` | Missing server certificate config fails before calling TTP |
| `TtpClientTest` | HTTP POST target, serialized forwarded payload, successful response decoding, TTP server error handling |

## Verification

Run:

```bash
mvn -pl server-service -am test
```

The command could not be executed in the Codex shell because Maven is not available on `PATH`.
