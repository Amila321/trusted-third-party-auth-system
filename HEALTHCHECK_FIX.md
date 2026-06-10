# Docker Healthcheck Fix

## Problem
Docker Compose healthcheck was failing even though services were running correctly.

**Error**: `dependency failed to start: container ... is unhealthy`

**Root Cause**: The healthcheck command uses `curl`, but `curl` was not installed in the Alpine-based Dockerfile images.

## Solution
Added `RUN apk add --no-cache curl` to all three backend Dockerfiles:

### Files Modified (3 total)
- ✅ `ttp-service/Dockerfile` - Added curl installation
- ✅ `server-service/Dockerfile` - Added curl installation  
- ✅ `client-backend/Dockerfile` - Added curl installation

## What Changed
```dockerfile
# BEFORE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
...

# AFTER
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl    # ← ADDED
WORKDIR /app
...
```

## To Apply the Fix

```bash
# 1. Stop current containers
docker-compose down

# 2. Clean Docker images (to force rebuild with new Dockerfiles)
docker system prune -f --all

# 3. Rebuild and start
docker-compose up --build
```

## Expected Result

Services will:
- ✅ Start successfully
- ✅ Pass healthchecks
- ✅ Become available to other containers
- ✅ All dependent containers will start

## Verification

```bash
# All should return 200 OK
curl http://localhost:8080/api/health
curl http://localhost:8081/api/health
curl http://localhost:8082/api/health

# Check container status
docker-compose ps
# All containers should show "healthy" and "(running)"
```

## Why This Works

Alpine Linux images are minimal and don't include `curl` by default. The `apk add --no-cache curl` command:
- Installs curl using the Alpine package manager (apk)
- `--no-cache` avoids caching to keep the image small
- Enables Docker's healthcheck command `curl -f http://localhost:PORT/api/health` to work correctly

---

**Status**: ✅ **Healthcheck fix complete**

