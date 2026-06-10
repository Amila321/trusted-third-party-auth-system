# Implementation Plan (Revised)

This plan extends the current repository skeleton into the required Trusted Third Party authentication scenario with encrypted client-server data exchange. It aligns strictly with Agents.md decisions: Docker-based virtualization (instructor-approved), in-memory TTP state (no MySQL for MVP), Spring Boot 3.3.5, Java 21, and Bouncy Castle cryptography.

## Architectural Foundation (from Agents.md)

- **Virtualization**: Docker Compose with `ttp-service` and `server-service` as independent containers, not classic VMs
- **Database**: In-memory only for MVP; MySQL removed from Docker environment
- **Security Dependencies**: Bouncy Castle `bcprov-jdk18on` and `bcpkix-jdk18on` (v1.78.1+) already available in `crypto-common`
- **Framework**: Spring Boot 3.3.5, Java 21, Maven multi-module structure

## Critical Pre-Phase Actions

### Action 0.1: Fix docker-compose.yml (MySQL Removal)

**Current Problem**: `docker-compose.yml` currently includes MySQL service, which violates Agents.md.

**Edit**: `docker-compose.yml`
- Remove the `mysql` service entirely
- Keep only: `ttp-service`, `server-service`, `client-backend`, `client-frontend`
- Specify internal Docker network for service-to-service communication

**Expected Result**:
```yaml
services:
  ttp-service:
    build: ./ttp-service
    ports:
      - "8080:8080"
    networks:
      - scs-network
  # ... other services ...
networks:
  scs-network:
    driver: bridge
```

### Action 0.2: Update README.md

**Edit**: `README.md`
- Remove mention of "ttp-service (Spring Boot + MySQL)"
- Clarify that MySQL is intentionally not used for the MVP
- Reference Agents.md for architecture decisions

---

## Phase 1: Container Runtime Setup & Configuration Profiles

**Goal**: Enable both local and Docker-based execution paths without hardcoding localhost URLs.

### Files to Edit/Create:

1. **Dockerfiles** (Create all four):
   - `ttp-service/Dockerfile`
   - `server-service/Dockerfile`
   - `client-backend/Dockerfile`
   - `client-frontend/Dockerfile`

   Example structure (common for Java services):
   ```dockerfile
   FROM eclipse-temurin:21-jre-alpine
   WORKDIR /app
   COPY target/*.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

2. **Configuration Profiles**:
   - `ttp-service/src/main/resources/application-docker.yml`
   - `server-service/src/main/resources/application-docker.yml`
   - `client-backend/src/main/resources/application-docker.yml`
   - `client-backend/src/main/resources/application-local.yml` (or default)

3. **Environment Variable Strategy**:
   - Add `SPRING_PROFILES_ACTIVE` environment variables (e.g., `docker` or `local`)
   - Docker Compose should pass `SPRING_PROFILES_ACTIVE=docker`

### Configuration Examples:

**application-docker.yml** (for services inside Docker Compose):
```yaml
server:
  port: 8080  # or 8081, 8082 respectively

services:
  server-service:
    base-url: http://server-service:8081  # Use service name, not localhost
  ttp-service:
    base-url: http://ttp-service:8080
```

**application-local.yml** (for developers running services locally):
```yaml
server:
  port: 8080  # Same port, but consumed from localhost

services:
  server-service:
    base-url: http://localhost:8081
  ttp-service:
    base-url: http://localhost:8080
```

### Tasks:

1. Create Dockerfiles for all Java services and client-frontend (using Node alpine image).
2. Create `application-docker.yml` for each service with Docker-internal service names.
3. Update `docker-compose.yml` to:
   - Reference build contexts and Dockerfiles
   - Map ports: `ttp-service: 8080:8080`, `server-service: 8081:8081`, `client-backend: 8082:8082`, `client-frontend: 5173:5173`
   - Define a shared Bridge network named `scs-network`
   - Set `SPRING_PROFILES_ACTIVE=docker` environment variable for Java services
4. Add Maven build step to documentation (run `mvn clean package` before Docker Compose).
5. Update `README.md` with verified local and Docker run instructions.

### Result Verification:
- `mvn -pl ttp-service spring-boot:run` works locally on port 8080
- `docker-compose up` starts all services on correct ports with service-name networking

---

## Phase 2: Define Shared DTO Contracts

**Goal**: Establish clear request/response objects for all inter-service communication.

### Files to Create in `dto-common/src/main/java/com/scs/dto/`:

#### Registration DTOs:
- `auth/RegistrationRequest.java` - Base registration data (identity name, public key)
- `auth/RegistrationResponse.java` - Returns identity ID and issued X.509 certificate (PEM format)
- `auth/LoginRequest.java` - Identity ID for re-authentication
- `auth/LoginResponse.java` - Current certificate and session challenge

#### Authentication DTOs:
- `auth/ServerAuthenticationRequest.java` - Server forwards User auth request to TTP (User ID, signed challenge, certificate)
- `auth/TtpAuthenticationDecision.java` - TTP returns OK/FAIL and encrypted session keys for both User and Server
- `auth/UserAuthenticationRequest.java` - User sends signed challenge and Server reference to TTP via Server

#### Session & Data Exchange DTOs:
- `session/SessionKeyResponse.java` - Encrypted AES-256 session key (separate encryption for User and Server)
- `data/EncryptedDataRequest.java` - Encrypted payload, optional session counter/nonce
- `data/EncryptedDataResponse.java` - Encrypted response payload
- `common/ErrorResponse.java` - Standard error responses (code, message, details)

### Technical Specifications for DTOs:

**Public Key Format**: PEM-encoded X.509 SubjectPublicKeyInfo (PKCS#8)
**Certificate Format**: PEM-encoded X.509 v3
**Session Key**: Base64-encoded AES-256 key encrypted with recipient RSA public key
**Encrypted Data**: Base64-encoded ciphertext (AES-256-CBC or -GCM as per Phase 3)
**Timestamps**: ISO 8601 format, UTC

### Tasks:

1. Create all DTO classes in `dto-common` with:
   - `@Data` (Lombok)
   - Jackson `@JsonProperty` annotations for explicit field names
   - `@NotNull` and `@NotBlank` validation annotations
2. Do NOT add business logic to DTOs; keep them as pure data carriers.
3. Ensure all services depend on `dto-common`.
4. Add unit tests for DTO serialization/deserialization in `dto-common/src/test/java/`.

---

## Phase 3: Implement Cryptographic Utilities

**Goal**: Centralize RSA, AES, hashing, and X.509 certificate operations in `crypto-common`.

### Files to Create in `crypto-common/src/main/java/com/scs/crypto/`:

#### Constants:
- `config/CryptoConstants.java`:
  ```java
  public static final int RSA_KEY_SIZE = 4096;
  public static final int AES_KEY_SIZE = 256;
  public static final String AES_ALGORITHM = "AES";
  public static final String AES_CIPHER_MODE = "AES/CBC/PKCS5Padding";  // or GCM
  public static final String RSA_CIPHER_MODE = "RSA/ECB/PKCS1Padding";
  public static final String HASH_ALGORITHM = "SHA-256";
  public static final int CERTIFICATE_VALIDITY_DAYS = 365;
  public static final String X509_VERSION = "X.509";
  ```

#### RSA Key Management:
- `rsa/RsaKeyService.java`:
  - `KeyPair generateKeyPair()` - Generate RSA 4096
  - `String encodePublicKeyPem(PublicKey)` - Convert to PEM
  - `PublicKey decodePublicKeyPem(String)` - Parse PEM
  - `String encodePrivateKeyPem(PrivateKey)` - Convert to PEM (for TTP only)
  - `PrivateKey decodePrivateKeyPem(String)` - Parse PEM (for TTP only)

#### RSA Encryption/Decryption:
- `rsa/RsaEncryptionService.java`:
  - `byte[] encrypt(byte[] plaintext, PublicKey)` - RSA encrypt
  - `byte[] decrypt(byte[] ciphertext, PrivateKey)` - RSA decrypt

#### AES Key Generation:
- `aes/AesKeyService.java`:
  - `SecretKey generateSessionKey()` - Generate secure random AES-256 key
  - `String encodeKey(SecretKey)` - Base64 encode
  - `SecretKey decodeKey(String)` - Base64 decode

#### AES Encryption/Decryption:
- `aes/AesEncryptionService.java`:
  - `byte[] encrypt(byte[] plaintext, SecretKey, byte[] iv)` - AES encrypt
  - `byte[] decrypt(byte[] ciphertext, SecretKey, byte[] iv)` - AES decrypt
  - `byte[] generateIv()` - Generate random IV

#### Hashing:
- `hash/HashService.java`:
  - `String hashIdentity(String identityName)` - SHA-256 hash of identity (e.g., user ID)
  - `String hashChallenge(String challenge, String salt)` - For signed challenge validation

#### X.509 Certificate Generation & Validation:
- `certificate/CertificateService.java`:
  - `X509Certificate generateCertificate(PublicKey publicKey, PrivateKey caPrivateKey, String subjectDN, int validityDays)` - Issue cert (TTP only)
  - `String encodeCertificatePem(X509Certificate)` - Convert to PEM
  - `X509Certificate decodeCertificatePem(String)` - Parse PEM
  - `boolean validateCertificate(X509Certificate, PublicKey caTrustAnchor)` - Verify signature
  - `boolean isCertificateExpired(X509Certificate)` - Check expiry
  - `PublicKey extractPublicKeyFromCertificate(X509Certificate)` - Get public key
  - `String extractSubjectDN(X509Certificate)` - Get identity reference

#### Base64 Encoding:
- `encoding/EncodingService.java`:
  - `String encodeBase64(byte[])` - Encode bytes
  - `byte[] decodeBase64(String)` - Decode string

### pom.xml Update:

Add test dependencies to `crypto-common/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
</dependency>
```

### Tests to Add:

- `RsaKeyServiceTest`: Key generation, PEM encoding/decoding
- `RsaEncryptionServiceTest`: Encryption/decryption round-trip
- `AesKeyServiceTest`: Key generation, encoding/decoding
- `AesEncryptionServiceTest`: Encryption/decryption with IV
- `HashServiceTest`: Consistent hashing
- `CertificateServiceTest`: Generation, validation, expiry checks
- `EncodingServiceTest`: Base64 encode/decode

### Tasks:

1. Implement all cryptographic services using Bouncy Castle where necessary.
2. Make all services stateless and thread-safe.
3. Write unit tests covering happy path and error cases.
4. Add inline Javadoc for cryptographic assumptions.

---

## Phase 4: Implement TTP Registration & Certificate Issuance

**Goal**: Register User/Server identities and issue certificates.

### Files to Create in `ttp-service`:

#### Models:
- `src/main/java/com/scs/ttp/model/RegisteredIdentity.java` (Lombok `@Data`):
  ```java
  - String identityId (unique hash)
  - String identityName (original name)
  - IdentityType type (USER or SERVER)
  - PublicKey publicKey
  - X509Certificate certificate
  - Instant registeredAt
  - Instant certificateExpiresAt
  ```

#### Services:
- `src/main/java/com/scs/ttp/service/TtpCertificateAuthority.java`:
  - `KeyPair getTtpKeyPair()` - Load or generate TTP signing key (stored in `application.yml` encrypted or environment secret)
  - `X509Certificate signCertificate(PublicKey subjectKey, String subjectDN)` - Use TTP private key to sign

- `src/main/java/com/scs/ttp/service/InMemoryIdentityStore.java`:
  - `Map<String, RegisteredIdentity>` to store identities
  - `RegisteredIdentity registerIdentity(String name, PublicKey key, IdentityType type)`
  - `RegisteredIdentity getIdentity(String identityId)`
  - `boolean identityExists(String name)`

- `src/main/java/com/scs/ttp/service/RegistrationService.java`:
  - `RegistrationResponse registerUser(RegistrationRequest)`
  - `RegistrationResponse registerServer(ServerRegistrationRequest)`

#### Controller:
- `src/main/java/com/scs/ttp/controller/RegistrationController.java`:
  ```java
  POST /api/ttp/register/user
  POST /api/ttp/register/server
  GET  /api/ttp/certificate/{identityId}  // Retrieve certificate
  ```

#### Logging:
- Use SLF4J (default in Spring Boot) with `@Slf4j` (Lombok)
- Log: registration attempts, certificate issuance, errors with timestamps

#### Configuration:
- `src/main/resources/application.yml`:
  ```yaml
  ttp:
    certificate:
      validity-days: 365
    keypair:
      storage: environment  # or file for later
  ```

### Tasks:

1. Implement identity storage and registration logic.
2. Implement certificate generation/signing using TtpCertificateAuthority.
3. Prevent duplicate registrations by name.
4. Return signed X.509 certificates in PEM format.
5. Add comprehensive logging for registration and certificate operations.
6. Write tests for successful registration, duplicate registration rejection, and certificate retrieval.

---

## Phase 5: Implement Server-Side Service Request Handling

**Goal**: Allow User requests to reach Server; Server communicates auth data to TTP.

### Files to Create in `server-service`:

#### Services:
- `src/main/java/com/scs/server/service/TtpClient.java`:
  - Uses `RestClient` to call TTP authentication endpoints
  - Handles connection errors

- `src/main/java/com/scs/server/service/ServerAuthenticationService.java`:
  - Accepts User request (identity, challenge, certificate)
  - Forwards to TTP via `TtpClient`
  - Stores temporary session state (in-memory)

#### Controllers:
- `src/main/java/com/scs/server/controller/ServiceRequestController.java`:
  ```java
  POST /api/server/request
  (Payload: User ID, requested service, User certificate, signed challenge)
  ```

#### Configuration:
- Update `src/main/resources/application.yml`:
  ```yaml
  services:
    ttp-service:
      base-url: http://localhost:8080  # (or http://ttp-service:8080 for Docker)
  ```

#### Logging:
- Log incoming requests, TTP interactions, authentication decisions.

### Tasks:

1. Expose endpoint for User service requests.
2. Forward auth data to TTP and await decision.
3. Store session state temporarily (e.g., `ConcurrentHashMap<String, SessionContext>`).
4. Log all operations with timestamps.
5. Write tests for successful forwarding, TTP unavailable, and validation failures.

---

## Phase 6: Implement TTP Authentication & Session Key Distribution

**Goal**: Complete the authentication flow with session key generation and distribution.

### Files to Create in `ttp-service`:

#### Models:
- `src/main/java/com/scs/ttp/model/AuthenticationSession.java` (Lombok `@Data`):
  - String sessionId
  - String userId
  - String serverId
  - SecretKey aesSessionKey (store temporarily in memory)
  - Instant createdAt
  - boolean authenticated

#### Services:
- `src/main/java/com/scs/ttp/service/SessionKeyService.java`:
  - `SecretKey generateSessionKey()` - Generate AES-256
  - `String encryptSessionKeyForRecipient(SecretKey, PublicKey recipientPublicKey)` - RSA encrypt for User
  - `String encryptSessionKeyForRecipient(SecretKey, PublicKey recipientPublicKey)` - RSA encrypt for Server

- `src/main/java/com/scs/ttp/service/InMemorySessionStore.java`:
  - `Map<String, AuthenticationSession>` to store active sessions
  - `AuthenticationSession createSession(String userId, String serverId, SecretKey sessionKey)`
  - `AuthenticationSession getSession(String sessionId)`
  - `void closeSession(String sessionId)`

- `src/main/java/com/scs/ttp/service/AuthenticationService.java`:
  - `TtpAuthenticationDecision authenticateUserForServer(String userId, String serverId, X509Certificate userCert, String signedChallenge)`
  - Validates certificates against stored ones
  - Validates signatures
  - Generates session key if successful
  - Returns encrypted session keys

#### Controllers:
- Update `src/main/java/com/scs/ttp/controller/AuthenticationController.java`:
  ```java
  POST /api/ttp/auth/user
  (Payload: user ID, certificate, sign challenge)
  
  POST /api/ttp/auth/server
  (Payload: server ID, certificate)
  
  POST /api/ttp/auth/validate
  (Payload: user ID, server ID)
  Returns: encrypted session keys for both
  ```

#### Logging:
- Log authentication decisions (accepted/rejected), session key generation, session closure.

### Files to Create in `client-backend`:

#### Services:
- `src/main/java/com/scs/clientbackend/service/TtpClient.java`:
  - Similar to server's TtpClient

- `src/main/java/com/scs/clientbackend/service/ClientAuthenticationService.java`:
  - Initiates User authentication with TTP
  - Stores decrypted session key locally after symmetric decryption

#### Controllers:
- `src/main/java/com/scs/clientbackend/controller/AuthenticationController.java`:
  ```java
  POST /api/client/auth/initiate
  (Payload: user identity request)
  
  POST /api/client/auth/complete
  (Payload: encrypted session key from TTP)
  Returns: decrypted session key for subsequent data exchange
  ```

### Tasks:

1. Implement TTP endpoints for User authentication validation.
2. Implement TTP endpoints for Server authentication validation.
3. Implement session key generation and encryption.
4. Implement client-backend authentication flow calling TTP.
5. Store session key in-memory on client-backend for the active session.
6. Add comprehensive logging and error handling.
7. Write tests for successful authentication, certificate validation failures, and invalid signatures.

---

## Phase 7: Implement Encrypted Client-Server Data Exchange

**Goal**: Exchange encrypted data between User and Server after authentication.

### Files to Create in `client-backend`:

#### Services:
- `src/main/java/com/scs/clientbackend/service/DataExchangeService.java`:
  - Encrypts plaintext using stored session key
  - Sends to Server
  - Decrypts response from Server

- `src/main/java/com/scs/clientbackend/service/ServerConnection.java`:
  - HttpClient/RestClient for server-service calls

#### Controllers:
- `src/main/java/com/scs/clientbackend/controller/DataExchangeController.java`:
  ```java
  POST /api/client/data/encrypt-and-send
  (Payload: plaintext, service ID)
  Returns: encrypted response
  
  POST /api/client/session/close
  (Payload: session ID)
  ```

### Files to Create in `server-service`:

#### Services:
- `src/main/java/com/scs/server/service/DataExchangeService.java`:
  - Decrypts incoming data using stored session key
  - Encrypts response

#### Controllers:
- `src/main/java/com/scs/server/controller/DataExchangeController.java`:
  ```java
  POST /api/server/data/decrypt-and-process
  (Payload: encrypted ciphertext)
  Returns: encrypted response
  
  POST /api/server/session/close
  (Payload: session ID)
  ```

#### Session Storage:
- In-memory `ConcurrentHashMap<String, SessionContext>` mapping session ID to decrypted AES key

### Logging:
- Log data received, decryption success, processing, encryption of response, session closure.

### Tasks:

1. Implement AES encryption/decryption of data payloads.
2. Implement session lookup and key retrieval.
3. Implement session closure and cleanup (e.g., expire after time).
4. Add tests for successful encrypted transfer, missing session, invalid ciphertext.

---

## Phase 8: Extend Frontend UI for Scenario Demonstration

**Goal**: Replace status dashboard with interactive scenario UI.

### Files to Create/Update in `client-frontend`:

#### Components:
- Extract existing `ServiceCard.jsx` from `App.jsx`
- Create `RegistrationPanel.jsx` - User inputs name, clicks "Register"
- Create `AuthenticationPanel.jsx` - Initiates auth flow, shows status
- Create `DataExchangePanel.jsx` - Text input for data to send, displays encrypted/decrypted results
- Create `LogPanel.jsx` - Scrollable event log of all actions and responses
- Create `SessionStatusBadge.jsx` - Visual indicator of auth state

#### API Layer:
- Create `src/api/clientApi.js`:
  - `registerUser(name)` → POST /api/client/auth/register
  - `authenticateWithServer(userId, serverId)` → POST /api/client/auth/complete
  - `sendEncryptedData(plaintext)` → POST /api/client/data/encrypt-and-send
  - `closeSession()` → POST /api/client/session/close

#### UI Layout:
- Header: "SCS TTP Authentication Demo"
- Two-column layout:
  - Left: Service Cards (status from `GET /api/client/status`)
  - Right: Scenario panels (Registration → Authentication → Data Exchange)
- Footer: Log panel showing event history

### Tasks:

1. Keep existing status dashboard functional in a separate section.
2. Implement scenario controls (buttons, input fields).
3. Call client-backend `/api/client/...` endpoints.
4. Capture and display all request/response events in log.
5. Show visual status for authentication state.

---

## Phase 9: Implement Negative Validation & Forged Certificate Scenario

**Goal**: Demonstrate rejection of invalid/forged certificates and authentication failures.

### Files to Create in `crypto-common`:

#### Test Utilities:
- `src/test/java/com/scs/crypto/certificate/InvalidCertificateGenerator.java`:
  - Generate expired certificates
  - Generate self-signed certificates (not signed by TTP)
  - Generate certificates with incorrect subject DNs

### Files to Create in `client-backend`:

#### Attack Simulation Service:
- `src/main/java/com/scs/clientbackend/service/AttackSimulationService.java`:
  - Generate a forged certificate (invalid signature, expired, wrong key)
  - Submit it to TTP auth endpoint
  - Log TTP rejection

#### Controller:
- `src/main/java/com/scs/clientbackend/controller/AttackSimulationController.java`:
  ```java
  POST /api/client/attack/submit-forged-cert
  Returns: TTP error response proving rejection
  ```

### Files to Create in `client-frontend`:

#### Component:
- `src/components/NegativeValidationPanel.jsx`:
  - Button: "Test Forged Certificate Rejection"
  - Display: TTP error response

### Tasks:

1. Implement negative test scenarios showing TTP rejection of invalid data.
2. Log rejection details (cert expiry, invalid signature, etc.).
3. Update frontend to display attack/rejection scenarios.
4. Write tests proving that forged certificates are rejected.

---

## Phase 10: Logging, Comprehensive Testing & Documentation

**Goal**: Prepare project for presentation and evaluation.

### Logging Configuration:

Update all `application.yml` files to enable structured logging:
```yaml
logging:
  level:
    com.scs: INFO                    # Application code
    org.springframework.web: DEBUG    # HTTP requests/responses
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
```

### Test Coverage Requirements:

- `crypto-common`: Unit tests for all crypto operations (80%+ coverage)
- `dto-common`: Serialization/deserialization tests
- `ttp-service`: Integration tests for full authentication flow
- `server-service`: Integration tests for request handling
- `client-backend`: Integration tests for User authentication and data exchange

### Documentation Files to Create:

1. **`docs/architecture.md`**:
   - System overview diagram (ASCII or reference to image)
   - Service responsibilities
   - Data flow diagram
   - Docker network topology

2. **`docs/api-specification.md`**:
   - Complete OpenAPI/Swagger specification or endpoint listing
   - All endpoints (TTP, Server, Client)
   - Request/response examples
   - HTTP status codes and error responses

3. **`docs/security-assumptions.md`**:
   - RSA-4096 encryption assumptions
   - AES-256 session key derivation
   - Certificate validity period
   - Hash algorithm (SHA-256)
   - Trusted root authorities

4. **`docs/test-plan.md`**:
   - Unit test coverage per module
   - Integration test scenarios
   - Negative test cases (forged certs, invalid signatures)
   - Docker Compose deployment test steps

5. **`docs/docker-deployment.md`**:
   - Prerequisites (Docker, Docker Compose)
   - Build and run instructions
   - Port mapping and network configuration
   - Log inspection commands

6. **`AGENTS.md`** notes in report:
   - Explain Docker-based virtualization as instructor-approved alternative to VMs
   - Reference Agents.md for architectural decisions

### Javadoc & Code Comments:

- Add Javadoc to all public classes and methods in backend modules
- Document cryptographic assumptions inline
- Add `@throws` documentation for exception-throwing methods

### Tasks:

1. Add SLF4J logging to all service classes (use `@Slf4j` from Lombok).
2. Configure log levels in application.yml for each service.
3. Write comprehensive unit tests for crypto-common (80%+ coverage).
4. Write integration tests for authentication and data exchange flows.
5. Prepare architecture and API documentation.
6. Add Docker deployment documentation.

---

## Phase 11: Docker Deployment Verification & Final Runbook

**Goal**: Ensure complete working Docker environment for presentation.

### Pre-Deployment Checklist:

1. ✓ All Dockerfiles created and tested locally
2. ✓ `docker-compose.yml` updated without MySQL
3. ✓ Service configuration profiles (local/docker) working
4. ✓ All endpoints tested locally before Docker build
5. ✓ Logs viewable from container output

### Docker Build & Deployment Steps:

```bash
# Build all Java artifacts
mvn clean package -DskipTests

# Start Docker Compose
docker-compose up --build

# Verify services are running
curl http://localhost:8080/api/health  # TTP
curl http://localhost:8081/api/health  # Server
curl http://localhost:8082/api/health  # Client-Backend
# Frontend: http://localhost:5173
```

### Inspection & Debugging:

```bash
# View service logs
docker-compose logs -f ttp-service
docker-compose logs -f server-service

# Connect to running container
docker exec -it ttp-service /bin/sh

# Stop all services
docker-compose down
```

### Tasks:

1. Build all Docker images successfully.
2. Verify network connectivity between services inside Docker.
3. Test complete authentication flow in Docker environment.
4. Verify frontend is accessible and shows service status.
5. Document the exact steps for the project presentation/demo.

---

## Optional Cleanup After MVP Works

### When to Do This: Only after full authentication flow is verified

Files to potentially edit:
- `ttp-service/pom.xml`
- `ttp-service/src/main/resources/application.yml`

Tasks:
1. Remove unused dependencies:
   ```xml
   <!-- REMOVE: spring-boot-starter-data-jpa -->
   <!-- REMOVE: mysql-connector-j -->
   ```
2. Remove JDBC/Hibernate exclusions if no longer needed.
3. Verify tests still pass after cleanup.

---

## Suggested Implementation Order

1. **Phase 0.1 & 0.2** (CRITICAL): Remove MySQL from docker-compose.yml and update README.md
2. **Phase 1**: Dockerfiles and configuration profiles (local vs. Docker)
3. **Phase 2**: Define all DTO contracts
4. **Phase 3**: Implement cryptographic utilities with tests
5. **Phase 4**: TTP registration and certificate issuance
6. **Phase 5**: Server request handling and TTP client
7. **Phase 6**: TTP authentication and session key distribution
8. **Phase 7**: Encrypted data exchange between User and Server
9. **Phase 8**: Extend frontend with scenario UI
10. **Phase 9**: Negative validation and forged certificate scenarios
11. **Phase 10**: Logging, comprehensive testing, documentation
12. **Phase 11**: Docker deployment verification and presentation runbook
13. (Optional) **Cleanup**: Remove unused JPA/MySQL dependencies after MVP verified

---

## Success Criteria

After completing all phases, the project should:
- ✓ Run in Docker Compose without MySQL
- ✓ Support User registration and Server registration through TTP
- ✓ Execute complete authenticated data exchange with AES-256 encryption
- ✓ Demonstrate rejection of forged/invalid certificates
- ✓ Provide timestamped logs of all cryptographic operations
- ✓ Display real-time scenario status on frontend
- ✓ Support both local and containerized execution
- ✓ Pass all unit and integration tests
- ✓ Be documentation-ready for project presentation

