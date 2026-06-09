# Implementation Plan

This plan is based on the current repository state and the project instruction for an emulated Trusted Third Party, Server, and User environment. The current repository already contains the initial multi-module structure, three Spring Boot services, a React/Vite frontend, shared Java modules, health endpoints, and an incomplete Docker Compose definition. The remaining work should extend this skeleton into the required authentication and encrypted data exchange scenario.

## Phase 1: Stabilize Local and Container Runtime

Goal: make the existing services easy to run in a repeatable way before adding security functionality.

Files to edit or create:

- `docker-compose.yml`
- `ttp-service/Dockerfile`
- `server-service/Dockerfile`
- `client-backend/Dockerfile`
- `client-frontend/Dockerfile`
- `client-backend/src/main/resources/application.yml`
- `ttp-service/src/main/resources/application.yml`
- `server-service/src/main/resources/application.yml`
- `README.md`

Tasks:

1. Add Dockerfiles for all declared Docker Compose services.
2. Configure Docker Compose ports for:
   - `ttp-service`: `8080`
   - `server-service`: `8081`
   - `client-backend`: `8082`
   - `client-frontend`: Vite or production frontend port
   - `mysql`: internal database port
3. Replace localhost service URLs inside containers with Docker service names, for example `http://server-service:8081` and `http://ttp-service:8080`.
4. Add profile-based configuration if both local and Docker execution are needed.
5. Configure MySQL connection properties for `ttp-service` once persistence is implemented.
6. Update `README.md` with the verified run commands.

## Phase 2: Define Shared DTO Contracts

Goal: create explicit request/response objects for registration, authentication, session establishment, and data exchange.

Files to edit or create:

- `dto-common/src/main/java/com/scs/dto/DtoMarker.java`
- `dto-common/src/main/java/com/scs/dto/registration/UserRegistrationRequest.java`
- `dto-common/src/main/java/com/scs/dto/registration/ServerRegistrationRequest.java`
- `dto-common/src/main/java/com/scs/dto/registration/RegistrationResponse.java`
- `dto-common/src/main/java/com/scs/dto/auth/ServiceRequestDto.java`
- `dto-common/src/main/java/com/scs/dto/auth/ServerAuthenticationRequest.java`
- `dto-common/src/main/java/com/scs/dto/auth/UserAuthenticationRequest.java`
- `dto-common/src/main/java/com/scs/dto/auth/AuthenticationDecisionResponse.java`
- `dto-common/src/main/java/com/scs/dto/session/SessionKeyResponse.java`
- `dto-common/src/main/java/com/scs/dto/data/EncryptedDataRequest.java`
- `dto-common/src/main/java/com/scs/dto/data/EncryptedDataResponse.java`

Tasks:

1. Create DTOs for User and Server registration with generated ID, public keys, and certificate-related data.
2. Create DTOs for Server authentication forwarding to TTP.
3. Create DTOs for User authentication response to TTP.
4. Create DTOs for encrypted session key delivery.
5. Create DTOs for encrypted User-Server data transfer.
6. Keep DTOs transport-focused and avoid business logic inside them.

## Phase 3: Implement Cryptographic Utilities

Goal: centralize cryptographic operations in `crypto-common` so the three services can reuse the same algorithms and constants.

Files to edit or create:

- `crypto-common/src/main/java/com/scs/crypto/CryptoMarker.java`
- `crypto-common/src/main/java/com/scs/crypto/config/CryptoConstants.java`
- `crypto-common/src/main/java/com/scs/crypto/rsa/RsaKeyService.java`
- `crypto-common/src/main/java/com/scs/crypto/aes/AesKeyService.java`
- `crypto-common/src/main/java/com/scs/crypto/aes/AesEncryptionService.java`
- `crypto-common/src/main/java/com/scs/crypto/hash/HashService.java`
- `crypto-common/src/main/java/com/scs/crypto/certificate/CertificateService.java`
- `crypto-common/src/main/java/com/scs/crypto/encoding/EncodingService.java`
- `crypto-common/src/test/java/com/scs/crypto/...`

Tasks:

1. Define constants for RSA `4096`, AES `256`, selected AES mode, block size assumptions, certificate validity, and hashing algorithm.
2. Implement RSA key pair generation and RSA encryption/decryption helpers.
3. Implement secure pseudorandom AES session key generation.
4. Implement AES encryption/decryption for User-Server data exchange.
5. Implement secure hash generation for public User and Server IDs.
6. Implement X.509 certificate generation and validation helpers using Bouncy Castle.
7. Add unit tests for RSA key generation, AES encryption/decryption, hash generation, certificate generation, and certificate validation.

## Phase 4: Implement TTP Registration and Certificate Issuance

Goal: make `ttp-service` responsible for registering User and Server identities and issuing public key certificates.

Files to edit or create:

- `ttp-service/src/main/java/com/scs/ttp/controller/RegistrationController.java`
- `ttp-service/src/main/java/com/scs/ttp/service/RegistrationService.java`
- `ttp-service/src/main/java/com/scs/ttp/service/TtpKeyService.java`
- `ttp-service/src/main/java/com/scs/ttp/model/RegisteredIdentity.java`
- `ttp-service/src/main/java/com/scs/ttp/repository/RegisteredIdentityRepository.java`
- `ttp-service/src/main/resources/application.yml`
- `ttp-service/src/test/java/com/scs/ttp/...`

Tasks:

1. Add endpoints for User and Server registration/login initiation.
2. Generate or load TTP key material needed to decrypt submitted IDs and issue certificates.
3. Store registered identities, public keys, issued certificates, and identity type in MySQL.
4. Return X.509 public key certificates to User and Server after successful registration.
5. Add timestamped logs for registration, certificate issuance, and validation events.
6. Add tests for successful registration, duplicate registration, invalid request data, and certificate generation.

## Phase 5: Implement Server-Side Service Request Flow

Goal: allow the User to request a service from `server-service`, and let the Server forward authentication information to TTP.

Files to edit or create:

- `server-service/src/main/java/com/scs/server/controller/ServiceRequestController.java`
- `server-service/src/main/java/com/scs/server/service/ServerAuthenticationService.java`
- `server-service/src/main/java/com/scs/server/service/TtpClient.java`
- `server-service/src/main/resources/application.yml`
- `server-service/src/test/java/com/scs/server/...`

Tasks:

1. Add an endpoint for receiving User service requests.
2. Forward the User request and Server authentication request to `ttp-service`.
3. Handle positive and negative TTP decisions.
4. Store temporary authentication/session state only for the current flow.
5. Add logs for received service requests, TTP forwarding, authentication decisions, and errors.
6. Add tests for successful forwarding, TTP unavailable, invalid certificate, and rejected authentication.

## Phase 6: Implement User Authentication and Session Key Distribution

Goal: complete the authentication flow where TTP validates User and Server and distributes an AES session key encrypted with their public keys.

Files to edit or create:

- `ttp-service/src/main/java/com/scs/ttp/controller/AuthenticationController.java`
- `ttp-service/src/main/java/com/scs/ttp/service/AuthenticationService.java`
- `ttp-service/src/main/java/com/scs/ttp/service/SessionKeyService.java`
- `client-backend/src/main/java/com/scs/clientbackend/controller/AuthenticationController.java`
- `client-backend/src/main/java/com/scs/clientbackend/service/ClientAuthenticationService.java`
- `client-backend/src/main/java/com/scs/clientbackend/service/TtpClient.java`
- `client-backend/src/main/resources/application.yml`
- `ttp-service/src/test/java/com/scs/ttp/...`
- `client-backend/src/test/java/com/scs/clientbackend/...`

Tasks:

1. Add TTP endpoints for Server authentication validation and User authentication validation.
2. Add client-backend logic for sending User authentication data to TTP.
3. Generate the AES-256 session key after successful validation.
4. Encrypt the session key separately for User and Server using their public keys.
5. Return OK/failed authentication status to both sides.
6. Add tests for successful authentication, incorrect User certificate, incorrect Server certificate, invalid ID, and failed decryption.

## Phase 7: Implement Encrypted Client-Server Data Exchange

Goal: demonstrate that User and Server can exchange encrypted data after receiving a valid session key.

Files to edit or create:

- `client-backend/src/main/java/com/scs/clientbackend/controller/DataExchangeController.java`
- `client-backend/src/main/java/com/scs/clientbackend/service/DataExchangeService.java`
- `client-backend/src/main/java/com/scs/clientbackend/service/ServerClient.java`
- `server-service/src/main/java/com/scs/server/controller/DataExchangeController.java`
- `server-service/src/main/java/com/scs/server/service/DataExchangeService.java`
- `server-service/src/test/java/com/scs/server/...`
- `client-backend/src/test/java/com/scs/clientbackend/...`

Tasks:

1. Store the active decrypted session key for the current session in User and Server runtime state.
2. Encrypt sample data in `client-backend` before sending it to `server-service`.
3. Decrypt received data in `server-service` and optionally return encrypted response data.
4. Close the session after the data exchange is finished.
5. Ensure a new service request repeats the authentication process.
6. Add tests for encrypted payload transfer, invalid session key, missing session, and session close behavior.

## Phase 8: Extend the Frontend From Status Dashboard to Scenario UI

Goal: provide a GUI for the required demonstration scenario while preserving the existing status dashboard.

Files to edit or create:

- `client-frontend/src/App.jsx`
- `client-frontend/src/App.css`
- `client-frontend/src/api/clientBackendApi.js`
- `client-frontend/src/components/ServiceCard.jsx`
- `client-frontend/src/components/ScenarioPanel.jsx`
- `client-frontend/src/components/LogPanel.jsx`
- `client-frontend/src/components/StatusBadge.jsx`

Tasks:

1. Extract the existing service card into a separate component.
2. Add controls for registration, authentication, service request, data exchange, and session close.
3. Add visible status/messages icons for authentication success/failure, server connection, TTP validation, and active session state.
4. Display a simple event log from frontend actions and backend responses.
5. Keep API calls routed through `client-backend` instead of calling `ttp-service` or `server-service` directly from the browser.

## Phase 9: Add Forged Certificate and Negative Validation Scenarios

Goal: demonstrate correct and incorrect validation, including resistance to a forged-certificate scenario.

Files to edit or create:

- `crypto-common/src/main/java/com/scs/crypto/certificate/CertificateService.java`
- `ttp-service/src/main/java/com/scs/ttp/service/AuthenticationService.java`
- `client-backend/src/main/java/com/scs/clientbackend/controller/AttackSimulationController.java`
- `client-backend/src/main/java/com/scs/clientbackend/service/AttackSimulationService.java`
- `client-frontend/src/components/AttackSimulationPanel.jsx`
- `client-frontend/src/App.jsx`
- related test files under `crypto-common`, `ttp-service`, and `client-backend`

Tasks:

1. Create a controlled forged/invalid certificate scenario for demonstration.
2. Ensure TTP rejects forged certificates and invalid identity data.
3. Return clear validation failure responses to the client-backend and frontend.
4. Add frontend controls for running the negative validation scenario.
5. Add tests proving that forged certificates are rejected.

## Phase 10: Logging, Tests, and Documentation

Goal: prepare the project for presentation and reporting.

Files to edit or create:

- `ttp-service/src/main/resources/application.yml`
- `server-service/src/main/resources/application.yml`
- `client-backend/src/main/resources/application.yml`
- `README.md`
- `docs/architecture.md`
- `docs/test-plan.md`
- `Doxyfile`
- Java source files requiring Doxygen/Javadoc-compatible documentation
- test files across all backend modules

Tasks:

1. Configure timestamped logs for `ttp-service` and `server-service`.
2. Add meaningful application logs for registration, authentication, validation, session key generation, data exchange, errors, and session closing.
3. Add integration tests for the complete positive flow.
4. Add integration tests for rejected authentication and forged certificate validation.
5. Add documentation for the architecture, endpoints, cryptographic assumptions, and run instructions.
6. Add Doxygen configuration and document the main backend classes/functions.
7. Prepare a short technical test plan covering authentication validation, sample attacks/tests, encryption/decryption, and network connections.

## Phase 11: VM/Network Deployment Preparation

Goal: align the implementation with the required environment using at least two virtual machines.

Files to edit or create:

- `docker-compose.yml`
- `docs/deployment-vm.md`
- `docs/network-environment.md`
- service configuration files under `src/main/resources/application.yml`

Tasks:

1. Document the planned VM split, for example TTP on one VM and Server on another VM.
2. Define hostnames/IP addresses and ports used by each service.
3. Configure service base URLs for VM deployment.
4. Verify network connectivity between User physical machine, Server VM, and TTP VM.
5. Document the final presentation runbook.

## Suggested Implementation Order

1. Complete Docker/local runtime setup.
2. Implement shared DTOs.
3. Implement cryptographic utilities and tests.
4. Implement TTP registration and certificate issuance.
5. Implement Server authentication forwarding.
6. Implement User authentication and session key distribution.
7. Implement encrypted data exchange.
8. Extend frontend scenario UI.
9. Add forged certificate/negative validation scenario.
10. Add logs, tests, Doxygen documentation, and report-supporting documentation.
11. Prepare VM deployment documentation and final demonstration steps.