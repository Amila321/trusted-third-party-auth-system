# Phase 8: Frontend Scenario Demonstration UI

## Overview

The React/Vite frontend now presents an educational TTP authentication workspace instead of only a service status dashboard. The UI keeps each protocol step visible so the user can observe what is sent, what is received, and which cryptographic state is produced.

## Visible Workflow

| Step | UI Panel | Backend Endpoint |
|---|---|---|
| Service health | Service status rail | `GET /api/client/status` |
| User registration | Register User at TTP | `POST /api/client/auth/initiate` |
| Session completion | Complete TTP Session | `POST /api/client/auth/complete` |
| Encrypted data exchange | Encrypted Data Exchange | `POST /api/client/data/encrypt-and-send` |
| Session closure | Encrypted Data Exchange | `POST /api/client/session/close` |

## Educational Visibility

- The protocol timeline shows Registration -> TTP decision -> Client completion -> Encrypted data.
- The registration panel displays the identity id, public key PEM, TTP certificate PEM, and registration timestamp returned by the backend.
- The authentication panel makes the TTP handoff explicit: paste `session_id` and `encrypted_session_key_for_user` from the TTP decision so the client backend can decrypt the AES key.
- The encrypted data panel shows encrypted request JSON, encrypted server response JSON, and decrypted response plaintext.
- The event log records every action, request payload, response payload, and error response in chronological order.

## Files Added

| File | Purpose |
|---|---|
| `client-frontend/src/api/clientApi.js` | Axios API wrapper for client-backend endpoints |
| `client-frontend/src/components/ServiceCard.jsx` | Service status display |
| `client-frontend/src/components/SessionStatusBadge.jsx` | Authentication/session state display |
| `client-frontend/src/components/ProtocolTimeline.jsx` | Visual protocol progress |
| `client-frontend/src/components/RegistrationPanel.jsx` | User registration flow |
| `client-frontend/src/components/AuthenticationPanel.jsx` | Manual TTP session-key completion flow |
| `client-frontend/src/components/DataExchangePanel.jsx` | Encrypted client-server data exchange |
| `client-frontend/src/components/LogPanel.jsx` | Request/response event log |
| `client-frontend/src/components/JsonBlock.jsx` | JSON artifact display |

## Files Updated

| File | Purpose |
|---|---|
| `client-frontend/src/App.jsx` | Scenario orchestration and application state |
| `client-frontend/src/App.css` | Full responsive scenario UI styling |
| `client-frontend/src/index.css` | Reset global Vite template styles |

## Verification

Executed successfully:

```bash
npm.cmd run lint
npm.cmd run build
```
