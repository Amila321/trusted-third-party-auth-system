# SCS Project

Project for the Security of Computer Systems course.

## Architecture

- **client-frontend** (React/Vite) - User-facing UI for the TTP authentication scenario
- **client-backend** (Spring Boot) - User-side backend service (port 8082)
- **server-service** (Spring Boot) - Server node (port 8081)
- **ttp-service** (Spring Boot) - Trusted Third Party service (port 8080)

## Design Decisions

**Virtualization**: Docker Compose with independent containers for TTP and Server services (instructor-approved alternative to classic VMs).

**Database**: In-memory session/identity storage only. MySQL is intentionally NOT used in this MVP. See `Agents.md` for detailed architectural decisions.

**Cryptography**: Bouncy Castle (RSA-4096, AES-256, X.509 certificates, SHA-256 hashing)

## Goal

Emulation of a Trusted Third Party environment with secure User-Server authentication and encrypted data exchange scenario.

## Running Locally (Development)

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js & npm

### Build
```bash
mvn clean install
```

### Start Services (Local - localhost addresses)

In separate terminals:

```bash
# Terminal 1: TTP Service
mvn -pl ttp-service spring-boot:run

# Terminal 2: Server Service
mvn -pl server-service spring-boot:run

# Terminal 3: Client Backend
mvn -pl client-backend spring-boot:run

# Terminal 4: Client Frontend
cd client-frontend && npm install && npm run dev
```

Verify:
- TTP: `curl http://localhost:8080/api/health`
- Server: `curl http://localhost:8081/api/health`
- Client-Backend: `curl http://localhost:8082/api/health`
- Frontend: http://localhost:5173

## Running with Docker Compose (Production-like)

### Prerequisites
- Docker
- Docker Compose

### Build & Run

```bash
# Build all artifacts first
mvn clean package -DskipTests

# Start all services in Docker
docker-compose up --build
```

Verify:
- TTP: `curl http://localhost:8080/api/health`
- Server: `curl http://localhost:8081/api/health`
- Client-Backend: `curl http://localhost:8082/api/health`
- Frontend: http://localhost:5173
- Check logs: `docker-compose logs -f ttp-service`
- Stop: `docker-compose down`

## Attack Simulation Demo

After the normal positive flow works in the frontend, use the `Attack Simulation` panel to demonstrate negative validation:

1. Refresh status and confirm TTP, Server, and Client Backend are `UP`.
2. Register User at TTP.
3. Register Server at TTP.
4. Run `Request and complete session` to prove the valid flow.
5. Send an encrypted message to prove AES data exchange works.
6. Run `Forged User Certificate Attack`.
7. Confirm the frontend shows `authenticated: false` and a rejection reason such as `Certificate was not signed by the TTP`.
8. Run `Invalid Challenge Signature Attack`.
9. Confirm the frontend shows `authenticated: false` and `Invalid signed challenge`.

Attack requests still use the real pipeline:

```text
client-frontend -> client-backend -> server-service -> ttp-service
```

The attack simulation does not expose private keys, does not create a valid AES session when TTP rejects the request, and does not overwrite the current valid User or Server identity.

Relevant endpoint:

```http
POST /api/client/attack/simulate
```

## Architecture Reference

See `Agents.md` for:
- Detailed virtualization decisions
- Database (in-memory) decisions
- Current functional scope
- Technology stack

See `plan-improved.prompt.md` for:
- Detailed implementation roadmap (11 phases)
- Success criteria
- Technical specifications for DTOs, cryptography, endpoints
