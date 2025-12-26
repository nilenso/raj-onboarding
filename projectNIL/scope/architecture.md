# Architecture (Canonical)

## Goal
ProjectNIL is a Function-as-a-Service (FaaS) platform:
- Users submit **source code**.
- The platform compiles it to **WebAssembly (WASM)** asynchronously.
- Users execute the compiled function on demand with JSON input/output.

This doc defines the **service boundaries** and responsibilities end-to-end.

## System Context

### Components
- **Client**: any HTTP client invoking the API.
- **API Service (Spring Boot / Java)**: REST interface, persistence, orchestration, WASM execution.
- **PostgreSQL**:
  - Persistent tables: `functions`, `executions`
  - **pgmq queues**: `compilation_jobs`, `compilation_results`
- **Compiler Service(s)** (language-specific workers): consume compilation jobs, publish results.

### Primary Protocols
- **HTTP + JSON** between Client ↔ API Service.
- **pgmq (Postgres extension) + JSON payloads** between API Service ↔ Compiler Service(s).
- **WASM execution** within API service via a Java WASM runtime (Chicory).

## Service Responsibilities

### API Service
- Owns the **REST API** contract.
- Owns persistence of:
  - `Function` (source, status, wasm binary, compile errors)
  - `Execution` (inputs/outputs, status, runtime errors)
- Publishes `CompilationJob` messages.
- Consumes `CompilationResult` messages.
- Executes compiled WASM via `WasmRuntime`.

### Compiler Service(s)
- Consume `CompilationJob` messages.
- Compile based on `language`.
- Publish `CompilationResult` messages.
- Must be safe to run concurrently; multiple compilers can co-exist.

### PostgreSQL + pgmq
- Stores platform state (tables).
- Provides queues for asynchronous compilation.

## Ownership of Domain
- The **canonical domain entities** are `Function` and `Execution`.
- Status transitions are documented in `scope/entities.md`.

## Deployment View (Logical)

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

  Compiler[Compiler Service
(assemblyscript, future langs)] -->|pgmq read| Jobs
  Compiler -->|pgmq send| Results

  API -->|pgmq read| Results
  API -->|execute WASM| Wasm[WASM Runtime (Chicory)]
```

## Key Cross-Cutting Requirements (Canonical)
- **Correlation IDs**: `functionId` correlates compilation; `executionId` correlates execution.
- **Idempotency**: compilation result processing must be safe under redelivery.
- **Failure visibility**: failures must be persisted on the owning entity (`compileError` on `Function`, `errorMessage` on `Execution`).

## Non-Goals (Current Phase)
- Multi-tenant auth/permissions.
- Long-running/async executions.
- External artifact storage (e.g. S3).
