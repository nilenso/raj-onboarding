# Infrastructure Documentation

**Last Updated**: December 26, 2025

This document describes the infrastructure setup for ProjectNIL, including local development, production deployment, and CI/CD pipelines.

---

## 1. Overview

ProjectNIL uses a containerized architecture deployed via Podman Compose:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Production Server                             │
│                     (DigitalOcean Droplet)                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │   API       │  │  Compiler   │  │  PostgreSQL │                  │
│  │  :8080      │  │   :8081     │  │   :5432     │                  │
│  │             │  │             │  │  + pgmq     │                  │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
│         │                │                │                          │
│         └────────────────┴────────────────┘                          │
│                          │                                           │
│                   infra_default network                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Directory Structure

```
projectNIL/infra/
├── ansible/
│   └── provision.yml           # Server provisioning playbook
├── docker/
│   ├── api.Dockerfile          # API service image
│   └── compiler.Dockerfile     # Compiler service image
├── migrations/
│   ├── db.changelog-master.yaml
│   └── changelog/
│       ├── 001-create-functions-table.yaml
│       ├── 002-create-executions-table.yaml
│       └── 003-setup-pgmq-queues.yaml
├── compose.yml                 # Local development
└── prod.compose.yml            # Production deployment
```

---

## 3. Container Images

### API Service (`api.Dockerfile`)

Multi-stage build for the Spring Boot API:

```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :services:api:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
COPY --from=builder /app/services/api/build/libs/api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Environment Variables:**
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | - |
| `SPRING_DATASOURCE_USERNAME` | Database username | - |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - |
| `SPRING_THREADS_VIRTUAL_ENABLED` | Enable virtual threads | `true` |

### Compiler Service (`compiler.Dockerfile`)

Multi-stage build with Node.js for AssemblyScript:

```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY common/ common/
COPY services/ services/
RUN ./gradlew :services:compiler:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache nodejs npm && npm install -g assemblyscript
COPY --from=builder /app/services/compiler/build/libs/compiler-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/tmp/compiler
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
```

**Environment Variables:**
| Variable | Description | Default |
|----------|-------------|---------|
| `PGMQ_URL` | PostgreSQL JDBC URL for pgmq | `jdbc:postgresql://postgres:5432/projectnil` |
| `PGMQ_USERNAME` | Database username | `projectnil` |
| `PGMQ_PASSWORD` | Database password | `projectnil` |
| `ASC_BINARY` | AssemblyScript compiler binary | `asc` |
| `COMPILER_TMP_DIR` | Workspace directory | `/app/tmp/compiler` |

---

## 4. Compose Files

### Local Development (`compose.yml`)

Services:
- **postgres**: PostgreSQL 18 with pgmq extension
- **compiler**: Compiler service (profile: `full`)
- **liquibase**: Database migrations (profile: `migrate`)

```bash
# Start postgres only
podman compose up -d postgres

# Run migrations
podman compose --profile migrate up liquibase

# Start full stack (postgres + compiler)
podman compose --profile full up -d
```

### Production (`prod.compose.yml`)

All services start together with proper dependency ordering:

```
postgres (healthy) → liquibase (completed) → api + compiler
```

**Key differences from local:**
- No profiles (all services start)
- Restart policies (`unless-stopped`)
- Health checks for api and compiler
- Volume with `:Z` SELinux label

---

## 5. Database Migrations

Managed via Liquibase with YAML changelogs:

| Changelog | Purpose |
|-----------|---------|
| `001-create-functions-table.yaml` | Functions table with status enum |
| `002-create-executions-table.yaml` | Executions table with FK to functions |
| `003-setup-pgmq-queues.yaml` | Create pgmq extension and queues |

**Running migrations locally:**
```bash
podman compose --profile migrate up liquibase
```

**Running migrations manually:**
```bash
podman exec projectnil-db psql -U projectnil -d projectnil -c "SELECT * FROM databasechangelog;"
```

---

## 6. CI/CD Pipeline

### Workflow: `deployment.yml`

Triggered on push to `main`:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Build Images   │ →  │ Transfer to     │ →  │ Deploy &        │
│  (API+Compiler) │    │ Droplet (SCP)   │    │ Health Check    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

**Steps:**
1. Checkout code
2. Build API image → `api-image.tar`
3. Build Compiler image → `compiler-image.tar`
4. Copy to `/opt/projectnil/` on droplet
5. Load images with `podman load`
6. Run `podman compose up -d`
7. Wait for health checks (API :8080, Compiler :8081)
8. Cleanup tarballs

**Required Secrets:**
| Secret | Description |
|--------|-------------|
| `DROPLET_IP` | Server IP address |
| `DROPLET_USER` | SSH username (typically `root`) |
| `SSH_PRIVATE_KEY` | SSH private key for authentication |

---

## 7. Server Provisioning

### Prerequisites

1. DigitalOcean Droplet (Ubuntu 22.04+, x86_64)
2. SSH access configured
3. Ansible installed locally

### Bootstrap (One-time)

```bash
# Set GitHub secrets
gh secret set DROPLET_IP --body "YOUR_IP"
gh secret set DROPLET_USER --body "root"
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_ed25519

# Run Ansible provisioning
cd projectNIL/infra/ansible
ansible-playbook -i "YOUR_IP," -u root provision.yml
```

### What Ansible Provisions

- Podman and podman-compose (Python wrapper, for compatibility)
- UFW firewall rule for port 8080
- Directory structure at `/opt/projectnil/infra/migrations`

> **Note:** For local development, prefer `podman compose` (native) over `podman-compose` (Python wrapper).
> The native version is idempotent and handles existing containers correctly.

### Manual Verification

```bash
ssh root@YOUR_IP
podman --version          # Should show 4.x+
ufw status               # Should show 8080/tcp ALLOW
ls /opt/projectnil/      # Should exist
```

---

## 8. Health Checks

Both services expose health endpoints:

| Service | Endpoint | Port |
|---------|----------|------|
| API | `GET /health` | 8080 |
| Compiler | `GET /health` | 8081 |

**Compose health check configuration:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

---

## 9. Networking

### Container Network

All services join `infra_default` network with these aliases:
- `postgres` → PostgreSQL
- `api` → API service
- `compiler` → Compiler service

### Port Mappings

| Service | Internal | External |
|---------|----------|----------|
| API | 8080 | 8080 |
| Compiler | 8081 | 8081 |
| PostgreSQL | 5432 | 5432 (local only) |

---

## 10. Volumes

| Volume | Mount Point | Purpose |
|--------|-------------|---------|
| `postgres_data` (local) | `/var/lib/postgresql` | Database storage |
| `postgres_prod_data` (prod) | `/var/lib/postgresql:Z` | Database storage (SELinux) |

**Note:** PGMQ/Postgres 18+ requires `/var/lib/postgresql` (not `/var/lib/postgresql/data`).

---

## 11. Troubleshooting

### View Logs

```bash
# All services
podman compose logs -f

# Specific service
podman logs projectnil-api
podman logs projectnil-compiler
podman logs projectnil-db
```

### Database Issues

```bash
# Connect to database
podman exec -it projectnil-db psql -U projectnil -d projectnil

# Check pgmq queues
SELECT * FROM pgmq.read('compilation_jobs', 30, 10);
SELECT * FROM pgmq.read('compilation_results', 30, 10);

# Reset database (DATA LOSS)
podman compose down -v
```

### Deployment Failures

1. Check GitHub Actions logs
2. SSH into server and check:
   ```bash
   podman ps -a                    # Container status
   podman logs projectnil-api      # API logs
   podman logs projectnil-compiler # Compiler logs
   ```

### Common Issues

| Issue | Solution |
|-------|----------|
| Image not found | Ensure `podman load` succeeded |
| Health check timeout | Check service logs for startup errors |
| Database connection refused | Verify postgres is healthy first |
| Migrations failed | Check liquibase container logs |

---

## 12. Security Considerations

### Current (Phase 0)

- No authentication (internal use only)
- Database credentials in environment variables
- SSH key-based deployment

### Recommended for Production

- [ ] Move secrets to vault (e.g., HashiCorp Vault)
- [ ] Enable TLS/HTTPS via reverse proxy
- [ ] Restrict database to internal network only
- [ ] Add rate limiting
- [ ] Enable audit logging

---

## 13. Maintenance Commands

```bash
# Restart services
podman compose restart api compiler

# Update and redeploy (manual)
podman compose pull
podman compose up -d

# Clean up old images
podman image prune -f

# Check disk usage
podman system df

# Backup database
podman exec projectnil-db pg_dump -U projectnil projectnil > backup.sql
```

---

## 14. Related Documentation

- [Deployment Roadmap](./deployment-roadmap.md) - Initial deployment planning
- [Compiler Service](./compiler.md) - Compiler implementation details
- [WASM Runtime](./wasm-runtime.md) - WASM execution details
- [Session Handoff](./session-handoff.md) - Current project status
