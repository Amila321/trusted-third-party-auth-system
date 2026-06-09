# Project Architecture

This repository currently contains an early-stage skeleton for the Security of Computer Systems project: an emulated Trusted Third Party environment with a client, a server service, and a TTP service.

The codebase is organized as a Maven multi-module Java project with a separate React/Vite frontend.

## Current Architecture

- `pom.xml` is the parent Maven project. It defines `com.scs:scs-project`, Java 21, Spring Boot 3.3.5, and the modules `crypto-common`, `dto-common`, `ttp-service`, `server-service`, and `client-backend`.
- `crypto-common` is a shared Java module for cryptographic code. At the moment it contains only `CryptoMarker.java`, but its `pom.xml` already declares Bouncy Castle dependencies: `bcprov-jdk18on` and `bcpkix-jdk18on`.
- `dto-common` is a shared Java module for DTO contracts. At the moment it contains only `DtoMarker.java` and depends on `spring-web`.
- `ttp-service` is a Spring Boot service running on port `8080`. It currently exposes `GET /api/health`, returning the service name and `UP` status. Its dependencies still include Spring Data JPA and MySQL connector, but the current architecture should not use MySQL for the MVP. The current `application.yml` disables JDBC and Hibernate auto-configuration.
- `server-service` is a Spring Boot service running on port `8081`. It currently exposes `GET /api/health`, returning the service name and `UP` status. Its dependencies include Spring Web, Validation, Actuator, Lombok, and the shared modules.
- `client-backend` is a Spring Boot service running on port `8082`. It exposes `GET /api/health` and `GET /api/client/status`. The status endpoint calls the health endpoints of `server-service` and `ttp-service` using `RestClient` and returns an aggregated status response.
- `client-frontend` is a React/Vite application. It uses Axios to call `http://localhost:8082/api/client/status` and displays service status cards for the client backend, server service, and TTP service.
- `docker-compose.yml` declares services for `ttp-service`, `server-service`, `client-backend`, and `client-frontend`. MySQL is intentionally not part of the Docker Compose environment. The file currently points to build contexts, but Dockerfiles were not observed in the service directories, so the Docker Compose flow is not yet complete.
- `.gitignore` excludes Java build outputs, Node dependencies/build artifacts, environment files, logs, OS files, and selected IDE metadata.

## Virtualization Decision

The original project instruction mentions a network environment with at least two virtual machines, preferably for TTP and Server. In this project, Docker-based virtualization is used instead of classic VMs because this approach was accepted by the instructor.

The target runtime environment should therefore be based on Docker Compose:

- `ttp-service` should run as an independent container representing the TTP node.
- `server-service` should run as an independent container representing the Server node.
- `client-backend` and `client-frontend` may run locally during development or as separate containers during the final demonstration.
- Services should communicate through a Docker Compose network using service names such as `ttp-service` and `server-service`, not hardcoded `localhost` addresses inside containers.

This setup is treated as the project network environment and should be described in the report as an instructor-approved containerized alternative to the VM-based environment mentioned in the instruction.

## Database Decision

MySQL should not be used in the current project scope. The project requirements focus on the TTP authentication scenario, RSA 4096, X.509 certificates, AES-256 session keys, encrypted User-Server data exchange, logs, and validation scenarios. The task assumes only one User, so persistent relational storage is not required for the MVP.

For the MVP, TTP state should be stored in memory, for example in simple runtime maps for registered identities, issued certificates, and active sessions. This keeps the implementation focused on the security protocol instead of database configuration, migrations, persistence mapping, and containerized database setup.

If persistence is added later as an optional extension, it should be introduced only after the complete cryptographic and authentication flow is working.

## Current Functional Scope

The currently implemented functionality is limited to service health checks and a frontend status dashboard. The repository does not yet contain the full authentication flow, RSA/AES cryptographic operations, X.509 certificate issuance/validation, session key exchange, encrypted client-server data transfer, runtime TTP registration storage, or attack/forged-certificate validation scenarios.

## Technologies and Dependencies

- Java 21
- Maven multi-module build
- Spring Boot 3.3.5
- Spring Web
- Spring Validation
- Spring Actuator
- Spring Data JPA and MySQL connector are still declared in `ttp-service`, but should not be used for the MVP
- Lombok
- Bouncy Castle in `crypto-common`
- React 19
- Vite 8
- Axios
- ESLint
- Docker Compose declared for application services only, without MySQL
- Docker-based virtualization as the instructor-approved runtime environment for TTP and Server separation

## How to Run

### Prerequisites

Install the following tools locally:

- Java 21
- Maven 3.9 or newer
- Node.js and npm
- Docker/Docker Compose only if you plan to continue the container setup

No required environment variables are currently defined in the codebase. The service URLs used by `client-backend` are configured in `client-backend/src/main/resources/application.yml`:

```yaml
services:
  server-service:
    base-url: http://localhost:8081
  ttp-service:
    base-url: http://localhost:8080
```

### 1. Build Java modules

From the repository root, run:

```bash
mvn clean install
```

### 2. Start `ttp-service`

Open a terminal in the repository root and run:

```bash
mvn -pl ttp-service spring-boot:run
```

Verify it:

```bash
curl http://localhost:8080/api/health
```

Expected response shape:

```json
{"service":"ttp-service","status":"UP"}
```

### 3. Start `server-service`

Open another terminal in the repository root and run:

```bash
mvn -pl server-service spring-boot:run
```

Verify it:

```bash
curl http://localhost:8081/api/health
```

Expected response shape:

```json
{"service":"server-service","status":"UP"}
```

### 4. Start `client-backend`

Open another terminal in the repository root and run:

```bash
mvn -pl client-backend spring-boot:run
```

Verify it:

```bash
curl http://localhost:8082/api/health
```

Then verify the aggregated status endpoint:

```bash
curl http://localhost:8082/api/client/status
```

Expected response shape:

```json
{
  "clientBackend": {
    "service": "client-backend",
    "status": "UP"
  },
  "serverService": {
    "service": "server-service",
    "status": "UP"
  },
  "ttpService": {
    "service": "ttp-service",
    "status": "UP"
  }
}
```

### 5. Start `client-frontend`

Open another terminal and run:

```bash
cd client-frontend
npm install
npm run dev
```

Open the Vite development server URL displayed in the terminal, usually:

```text
http://localhost:5173
```

The frontend should display the current status of `client-backend`, `server-service`, and `ttp-service`.

### Current Docker Compose Status

The repository contains `docker-compose.yml`, but it currently references service build contexts without visible Dockerfiles in those service directories. MySQL has been intentionally removed from Docker Compose.

Docker Compose should be completed later as the instructor-approved virtualization environment. The final container setup should run at least `ttp-service` and `server-service` as independent containers on the same Docker network. Until Dockerfiles and container-specific service URLs are added, the safest current run path is the local Maven/npm workflow described above.