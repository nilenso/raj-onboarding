# ProjectNIL

A Function as a Service (FaaS) platform. Users submit source code, it compiles to WebAssembly, and executes on demand in a sandboxed environment.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              User Request                                   │
│      POST /functions { "language": "assemblyscript", "source": "..." }      │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       API Service (Spring Boot)                             │
│                                                                             │
│   REST API → Persist source → Publish to pgmq → Execute WASM (Chicory)      │
└───────────┬─────────────────────────────────────────────────────────────────┘
            │                                           ▲
            │ compilation_jobs                          │ compilation_results
            ▼                                           │
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PostgreSQL                                     │
│   Tables: functions, executions    |    pgmq: compilation_jobs, results     │
└───────────┬─────────────────────────────────────────────────────────────────┘
            │                                           ▲
            ▼                                           │
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Compiler Service (Node.js)                               │
│                    AssemblyScript → WASM via asc                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Services

| Service | Tech | Port | Purpose |
|---------|------|------|---------|
| api-service | Spring Boot / Java 25 | 8080 | REST API, DB, WASM execution |
| compiler-assemblyscript | Node.js | - | Compile AS → WASM |
| postgres | PostgreSQL 18 + pgmq | 5432 | Persistence + message queue |

## Local Development

Start PostgreSQL with pgmq pre-installed:

```bash
podman run -d --name pgmq-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  ghcr.io/pgmq/pg18-pgmq:latest
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

| Component | Technology | ADR |
|-----------|------------|-----|
| Runtime | Java 25, Spring Boot 3.4 | - |
| Database | PostgreSQL 16, Liquibase | - |
| Message Queue | pgmq | [ADR-002](./decisions/002-message-queue-pgmq.md) |
| WASM Runtime | Chicory | [ADR-001](./decisions/001-wasm-runtime.md) |
| Compiler | AssemblyScript (asc) | - |
| Containers | Podman Compose | - |

## Function Lifecycle

```
POST /functions
      │
      ▼
┌─────────┐  queue   ┌───────────┐  success  ┌─────────┐
│ PENDING │─────────▶│ COMPILING │──────────▶│  READY  │
└─────────┘          └───────────┘           └─────────┘
                          │                       │
                          │ error                 │ POST /execute
                          ▼                       ▼
                    ┌──────────┐           ┌───────────┐
                    │  FAILED  │           │ Execution │
                    └──────────┘           └───────────┘
```

## Database Schema

```sql
CREATE TABLE functions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    language        VARCHAR(50) NOT NULL,
    source          TEXT NOT NULL,
    wasm_binary     BYTEA,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    compile_error   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE executions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    function_id     UUID REFERENCES functions(id) ON DELETE SET NULL,
    input           JSONB NOT NULL,
    output          JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    metadata        JSONB
);
```

## Message Formats

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

## Project Structure

```
projectNIL/
├── services/
│   ├── api-service/                 # Spring Boot
│   │   ├── build.gradle
│   │   └── src/main/
│   │       ├── java/com/projectnil/api/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   ├── repository/
│   │       │   ├── model/
│   │       │   ├── messaging/
│   │       │   └── runtime/
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/changelog/
│   │
│   └── compiler-assemblyscript/     # Node.js
│       ├── package.json
│       └── src/
│
├── gradle/libs.versions.toml
├── compose.yml
└── docs/
    ├── README.md                    # This file
    ├── api.md                       # API reference
    ├── roadmap.md                   # Future phases
    └── decisions/                   # ADRs
```

## Related Docs

- [API Reference](./api.md)
- [Roadmap](./roadmap.md)
- [ADR-001: WASM Runtime](./decisions/001-wasm-runtime.md)
- [ADR-002: Message Queue](./decisions/002-message-queue-pgmq.md)
