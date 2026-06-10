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
