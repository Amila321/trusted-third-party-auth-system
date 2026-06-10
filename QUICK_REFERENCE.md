# Quick Reference: Local vs Docker Execution

## Side-by-Side Comparison

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LOCAL (Maven/npm)                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ Configuration File:      application.yml (default)                          │
│ Service Addressing:      localhost:PORT                                     │
│ TTP Service URL:         http://localhost:8080/api/health                   │
│ Server Service URL:      http://localhost:8081/api/health                   │
│ Client Backend URL:      http://localhost:8082/api/health                   │
│ Frontend URL:            http://localhost:5173                              │
│ Database:                In-memory (at runtime)                             │
│ Build:                   mvnw.cmd clean install                             │
│ Run (5 terminals):       See LOCAL RUN section below                        │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                      DOCKER (Docker Compose)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ Configuration File:      application-docker.yml (auto-loaded)               │
│ Service Addressing:      service-name:PORT (internal)                       │
│ TTP Service URL:         http://ttp-service:8080/api/health                 │
│ Server Service URL:      http://server-service:8081/api/health              │
│ Client Backend URL:      http://client-backend:8082/api/health              │
│ Frontend URL:            http://localhost:5173 (from host)                  │
│ Database:                In-memory (in container memory)                    │
│ Build:                   mvnw.cmd clean package -DskipTests                 │
│ Run (1 command):         docker-compose up --build                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

## LOCAL RUN (Development)

Terminal 1 - Build everything:
```bash
mvnw.cmd clean install
```

Terminal 2 - TTP Service:
```bash
mvnw.cmd -pl ttp-service spring-boot:run
# Runs on http://localhost:8080
```

Terminal 3 - Server Service:
```bash
mvnw.cmd -pl server-service spring-boot:run
# Runs on http://localhost:8081
# Connects to: http://localhost:8080 (TTP)
```

Terminal 4 - Client Backend:
```bash
mvnw.cmd -pl client-backend spring-boot:run
# Runs on http://localhost:8082
# Connects to: http://localhost:8080 (TTP) and http://localhost:8081 (Server)
```

Terminal 5 - Client Frontend:
```bash
cd client-frontend
npm install
npm run dev
# Runs on http://localhost:5173
# Calls: http://localhost:8082/api/client/status
```

## DOCKER RUN (Production-like)

Single command (one terminal):
```bash
# Step 1: Build all artifacts
mvnw.cmd clean package -DskipTests

# Step 2: Start all containers
docker-compose up --build
```

## File Structure

```
project-root/
├── docker-compose.yml                   # ✅ Restructured (no MySQL)
├── README.md                            # ✅ Updated
│
├── ttp-service/
│   ├── Dockerfile                       # ✅ Created
│   ├── pom.xml
│   └── src/main/resources/
│       ├── application.yml              # ✅ localhost (local dev)
│       └── application-docker.yml       # ✅ service names (Docker)
│
├── server-service/
│   ├── Dockerfile                       # ✅ Created
│   ├── pom.xml
│   └── src/main/resources/
│       ├── application.yml              # ✅ localhost (local dev)
│       └── application-docker.yml       # ✅ service names (Docker)
│
├── client-backend/
│   ├── Dockerfile                       # ✅ Created
│   ├── pom.xml
│   └── src/main/resources/
│       ├── application.yml              # ✅ localhost (local dev)
│       └── application-docker.yml       # ✅ service names (Docker)
│
└── client-frontend/
    ├── Dockerfile                       # ✅ Created (multi-stage Node.js)
    ├── package.json
    ├── vite.config.js
    └── src/
        └── api/clientApi.js             # Will call localhost:8082
```

## How Spring Profile Switching Works

### Local Development (Default)
```bash
mvnw.cmd -pl ttp-service spring-boot:run
# No SPRING_PROFILES_ACTIVE set
# → Spring loads: application.yml (localhost URLs)
```

### Docker Deployment
```yaml
# In docker-compose.yml:
environment:
  - SPRING_PROFILES_ACTIVE=docker
# → Spring loads: application.yml + application-docker.yml (service name URLs)
```

## Verification Commands

### Local
```bash
# Service health checks
curl http://localhost:8080/api/health  # TTP
curl http://localhost:8081/api/health  # Server
curl http://localhost:8082/api/health  # Client-Backend

# Aggregated status (from Client-Backend)
curl http://localhost:8082/api/client/status

# Frontend
open http://localhost:5173
```

### Docker
```bash
# View logs
docker-compose logs -f ttp-service
docker-compose logs -f server-service

# Access containers
docker exec -it ttp-service /bin/sh

# Check network
docker network ls | grep scs

# Stop
docker-compose down
```

## Database in This Setup

**Both Local and Docker**: In-memory only
- No MySQL container
- No database schema
- Simple Java `Map<>` or `HashMap` for storing:
  - Registered identities (TTP)
  - Active sessions (TTP)
  - Temporary auth state (Server)
  - Session keys (Client-Backend)

State is lost when services restart (as designed for MVP).

## Port Summary

| Service | Port | Local | Docker | Access |
|---------|------|-------|--------|--------|
| TTP | 8080 | ✅ localhost:8080 | ✅ internal:8080 | curl http://localhost:8080/api/health |
| Server | 8081 | ✅ localhost:8081 | ✅ internal:8081 | curl http://localhost:8081/api/health |
| Client-Backend | 8082 | ✅ localhost:8082 | ✅ internal:8082 | curl http://localhost:8082/api/health |
| Frontend | 5173 | ✅ localhost:5173 | ✅ localhost:5173 | open http://localhost:5173 |

## Common Issues & Solutions

### Issue: "Connection refused" from Client-Backend to Server
**Cause**: Using wrong URL (localhost instead of service name, or vice versa)
**Fix**: Check which mode you're in:
- Local: application.yml uses `http://localhost:8081`
- Docker: application-docker.yml uses `http://server-service:8081`

### Issue: Frontend shows "Cannot reach backend"
**Cause**: Frontend trying to reach localhost:8082, but backend not running
**Fix**: Make sure client-backend service is running first

### Issue: Docker container exits immediately
**Cause**: JAR file not found
**Fix**: Run `mvnw.cmd clean package -DskipTests` before `docker-compose up`

### Issue: Port 8080 already in use
**Solution**: Stop conflicting service or change port in application.yml

---

**Created By**: Phase 0 & Phase 1 Execution
**Date**: 2026-06-09
**Status**: ✅ Ready for Phase 2 (DTOs)

