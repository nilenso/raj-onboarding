# ProjectNIL

A Function as a Service (FaaS) platform. Users submit source code, it compiles to WebAssembly, and executes on demand in a sandboxed environment.

> **Note:** `projectNIL/scope/` is the canonical specification. This doc summarizes the system and focuses on operational context.

## Architecture

Canonical, end-to-end architecture lives in `projectNIL/scope/architecture.md`.

```mermaid
flowchart LR
  Client[Client] -->|HTTP JSON| API[API Service]

  subgraph DB[PostgreSQL]
    Tables[(functions, executions)]
    Jobs[[pgmq: compilation_jobs]]
    Results[[pgmq: compilation_results]]
  end

  API -->|JPA| Tables
  API -->|pgmq send| Jobs

  Compiler[Compiler Service\n(assemblyscript, future langs)] -->|pgmq read| Jobs
  Compiler -->|pgmq send| Results

  API -->|pgmq read| Results
  API -->|execute WASM| Wasm[WASM Runtime (Chicory)]
```

## Services

| Service | Tech | Port | Purpose |
|---------|------|------|---------|
| api-service | Spring Boot 4.x / Java 25 | 8080 | REST API, DB, WASM execution |
| compiler-assemblyscript | Node.js (Latest LTS) | - | Compile AS → WASM |
| postgres | PostgreSQL 18 + pgmq | 5432 | Persistence + message queue |

## Local Development

### Prerequisites

- [Podman](https://podman.io/) (or Docker)
- [Podman Compose](https://github.com/containers/podman-compose) (or Docker Compose)

### Quick Start

```bash
cd projectNIL/infra

# Start PostgreSQL with pgmq
podman compose up -d postgres

# Run database migrations
podman compose --profile migrate up liquibase

# Verify setup
podman exec projectnil-db psql -U projectnil -d projectnil -c "\dt"
```

### Common Commands

```bash
# View logs
podman compose logs -f postgres

# Stop services
podman compose down

# Reset database (destroy all data)
podman compose down -v

# Connect to database
podman exec -it projectnil-db psql -U projectnil -d projectnil
```

### Manual Setup (Alternative)

If you prefer to run PostgreSQL manually:

```bash
podman run -d --name pgmq-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  ghcr.io/pgmq/pg18-pgmq:v1.8.0
```

Connect and enable pgmq:

```bash
psql postgres://postgres:postgres@localhost:5432/postgres
```

```sql
CREATE EXTENSION pgmq;

-- Create queues
SELECT pgmq.create('compilation_jobs');
SELECT pgmq.create('compilation_results');
```

### pgmq Quick Reference

```sql
-- Send message
SELECT pgmq.send('compilation_jobs', '{"functionId": "...", "language": "assemblyscript", "source": "..."}');

-- Read message (invisible for 30s)
SELECT * FROM pgmq.read('compilation_jobs', 30, 1);

-- Delete after processing
SELECT pgmq.delete('compilation_jobs', 1);

-- Or archive for retention
SELECT pgmq.archive('compilation_jobs', 1);
```

## Tech Stack

| Component | Technology | Version | ADR / Details |
|-----------|------------|---------|---------------|
| Language | Java | 25 | - |
| Framework | Spring Boot | 4.0.0 (Spring 7) | - |
| Database | PostgreSQL | 18 | - |
| Message Queue | pgmq | 1.8.0 | [ADR-002](./decisions/002-message-queue-pgmq.md) |
| WASM Runtime | Chicory | 0.0.1 | [ADR-001](./decisions/001-wasm-runtime.md) |
| Migrations | Liquibase | 4.30 | - |
| Compiler | AssemblyScript | Latest | - |
| Containers | Podman Compose | - | - |

See [stack.md](./stack.md) for full rationale and library versions.

## Function Lifecycle

Canonical state machines and flows live in:
- `projectNIL/scope/entities.md`
- `projectNIL/scope/flows.md`

```mermaid
stateDiagram-v2
  [*] --> PENDING
  PENDING --> COMPILING
  COMPILING --> READY
  COMPILING --> FAILED
```

## Database Schema

Managed via Liquibase migrations in `infra/migrations/`.

```sql
-- Status enums
CREATE TYPE function_status AS ENUM ('PENDING', 'COMPILING', 'READY', 'FAILED');
CREATE TYPE execution_status AS ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED');

CREATE TABLE functions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    language        VARCHAR(50) NOT NULL,
    source          TEXT NOT NULL,
    wasm_binary     BYTEA,
    status          function_status NOT NULL DEFAULT 'PENDING',
    compile_error   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE executions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    function_id     UUID REFERENCES functions(id) ON DELETE CASCADE,
    input           JSONB,
    output          JSONB,
    status          execution_status NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_functions_name ON functions(name);
CREATE INDEX idx_functions_status ON functions(status);
CREATE INDEX idx_executions_function_id ON executions(function_id);
CREATE INDEX idx_executions_status ON executions(status);
CREATE INDEX idx_executions_created_at ON executions(created_at DESC);
```

## Message Formats

Canonical queue and HTTP contracts are captured in `projectNIL/scope/contracts.md`.

**Compilation Request** (API → Compiler):
```json
{
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "language": "assemblyscript",
  "source": "export function add(a: i32, b: i32): i32 { return a + b; }"
}
```

**Compilation Result** (Compiler → API):
```json
{
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "success": true,
  "wasmBinary": "AGFzbQEAAAA...",
  "error": null
}
```

## CI Operational Notes
- Gradle caching (`actions/cache@v4`) now persists `~/.gradle/caches` and `~/.gradle/wrapper` during the “CI - dev & feature branches” workflow.
- Cold run (commit `b0145a7`, cache miss) executed `./gradlew build --build-cache` in ~80 s and uploaded a ~276 MB cache.
- Warm run (empty commit `ci: trigger cache verification`, SHA `441010f`) restored that cache (log shows `Cache hit` and multiple `FROM-CACHE` tasks) and completed in ~22 s.
- Use empty commits when you need to validate cache health without changing code.

## Project Structure

```
projectNIL/
├── common/                          # Shared entities and utilities
│   └── entities/
│
├── services/
│   ├── api/                         # Spring Boot API service
│   │   └── build.gradle.kts
│   │
│   └── compiler-assembly-script/    # Node.js compiler service
│       └── build.gradle.kts
│
├── infra/                           # Infrastructure configuration
│   ├── compose.yml                  # Podman/Docker Compose
│   └── migrations/                  # Liquibase database migrations
│       ├── db.changelog-master.yaml
│       └── changelog/
│           ├── 001-create-functions-table.yaml
│           ├── 002-create-executions-table.yaml
│           └── 003-setup-pgmq-queues.yaml
│
├── docs/
│   ├── README.md                    # This file
│   ├── api.md                       # API reference
│   ├── roadmap.md                   # Future phases
│   ├── stack.md                     # Technology stack
│   └── decisions/                   # ADRs
│
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## Related Docs

- [API Reference](./api.md)
- [Roadmap](./roadmap.md)
- [Tech Stack](./stack.md)
- [ADR-001: WASM Runtime](./decisions/001-wasm-runtime.md)
- [ADR-002: Message Queue](./decisions/002-message-queue-pgmq.md)
