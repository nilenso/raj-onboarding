# Phase 0: Core FaaS (No Auth)

## Overview

Phase 0 delivers the core Function as a Service capabilities without authentication or authorization. All operations are publicly accessible.

**Entities in scope:** Function, Execution

> Note: Environment, User, Group, and authentication are deferred to Phase 1 to keep the initial scope minimal.

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             User Request                                        │
│         POST /functions { "language": "assemblyscript", "source": "..." }       │
└─────────────────────────────────────┬───────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         API Service (Spring Boot)                               │
│                                                                                 │
│   • REST API (CRUD for functions, executions)                                   │
│   • Persists function source with status=PENDING                                │
│   • Publishes compilation job to RabbitMQ                                       │
│   • Consumes compilation results, stores WASM binary                            │
│   • Executes WASM via Chicory runtime                                           │
└───────────┬─────────────────────────────────────────────────────────────────────┘
            │                                              ▲
            │ Publish: compile.{language}                  │ Consume: compilation.results
            ▼                                              │
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              RabbitMQ                                           │
│   Exchange: compilation.requests (topic)                                        │
│   Queue: compilation.results                                                    │
└───────────┬─────────────────────────────────────────────────────────────────────┘
            │                                              ▲
            │ Consume: compile.assemblyscript              │ Publish result
            ▼                                              │
┌─────────────────────────────────────────────────────────────────────────────────┐
│                  Compiler Service (Node.js)                                     │
│                                                                                 │
│   • Consumes compilation requests from queue                                    │
│   • Compiles source → WASM using asc (AssemblyScript compiler)                  │
│   • Publishes result (WASM binary or error) to results queue                    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             PostgreSQL                                          │
│   • functions: source code + compiled WASM binary                               │
│   • executions: execution history and results                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Services

| Service                 | Technology                  | Port        | Responsibility                            |
|-------------------------|-----------------------------|-------------|-------------------------------------------|
| api-service             | Spring Boot 4.0.0 / Java 25 | 8080        | REST API, database access, WASM execution |
| compiler-assemblyscript | Node.js                | N/A (queue) | Compile AssemblyScript → WASM             |
| rabbitmq                | RabbitMQ                | 5672, 15672 | Message broker                            |
| postgres                | PostgreSQL              | 5432        | Persistence                               |

### Function Lifecycle

```
                    POST /functions
                          │
                          ▼
┌─────────┐  queue     ┌───────────┐  success   ┌─────────┐
│ PENDING │───────────▶│ COMPILING │───────────▶│  READY  │
└─────────┘            └───────────┘            └─────────┘
                            │                        │
                            │ error                  │ POST /execute
                            ▼                        ▼
                      ┌──────────┐            ┌───────────┐
                      │  FAILED  │            │ Execution │
                      └──────────┘            └───────────┘
```

**States:**
- `PENDING`: Source received, compilation job not yet published
- `COMPILING`: Job published to queue, awaiting compiler response
- `READY`: WASM binary stored, function can be executed
- `FAILED`: Compilation failed (error stored in `compile_error`)

### Execution Lifecycle

```
POST /functions/{id}/execute
           │
           ▼
     ┌─────────┐  load WASM   ┌─────────┐  success   ┌───────────┐
     │ PENDING │─────────────▶│ RUNNING │───────────▶│ COMPLETED │
     └─────────┘              └─────────┘            └───────────┘
                                   │
                                   │ error
                                   ▼
                              ┌──────────┐
                              │  FAILED  │
                              └──────────┘
```

---

## Message Queue Design

### Exchange & Queues

| Component | Type  | Name                     | Purpose                             |
|-----------|-------|--------------------------|-------------------------------------|
| Exchange  | Topic | `compilation.requests`   | Route compilation jobs by language  |
| Queue     | -     | `compile.assemblyscript` | AssemblyScript compilation jobs     |
| Queue     | -     | `compilation.results`    | Compilation results (all languages) |

### Routing

- **Routing key pattern**: `compile.{language}`
- **Example**: Message with routing key `compile.assemblyscript` goes to the AssemblyScript compiler queue

### Message Formats

#### Compilation Request

Published by API service when a function is registered or updated.

```json
{
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "language": "assemblyscript",
  "source": "export function add(a: i32, b: i32): i32 { return a + b; }"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `functionId` | UUID | Yes | Function ID in database |
| `language` | String | Yes | Source language (routing key suffix) |
| `source` | String | Yes | Source code to compile |

#### Compilation Result

Published by compiler service after compilation attempt.

**Success:**
```json
{
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "success": true,
  "wasmBinary": "AGFzbQEAAAABBwFgAn9/AX8DAgEABwcBA2FkZAAACgkBBwAgACABagsAEARuYW1lAgkBAAIAAWEBAWI=",
  "error": null
}
```

**Failure:**
```json
{
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "success": false,
  "wasmBinary": null,
  "error": "ERROR AS100: Syntax error at line 1, column 42: Expected ';' but found 'return'"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `functionId` | UUID | Function ID (correlation) |
| `success` | Boolean | Whether compilation succeeded |
| `wasmBinary` | String (Base64) | Compiled WASM (if success) |
| `error` | String | Error message (if failure) |

---

## Compiler Interface Specification

Any compiler service (current or future) must adhere to this contract:

### Requirements

1. **Consume** from queue: `compile.{language}` (e.g., `compile.assemblyscript`, `compile.rust`)
2. **Publish** to queue: `compilation.results`
3. **Message format**: As specified above
4. **Behavior**:
   - Stateless (no local state between compilations)
   - Idempotent (same source → same output)
   - Timeout handling (fail gracefully if compilation hangs)

### Supported Languages (Phase 0)

| Language | Queue | Compiler | Status |
|----------|-------|----------|--------|
| AssemblyScript | `compile.assemblyscript` | `asc` (Node.js) | Phase 0 |
| Rust | `compile.rust` | `rustc` + `wasm-pack` | Future |
| Go | `compile.go` | TinyGo | Future |

Adding a new language requires:
1. Create new compiler service
2. Bind to appropriate queue (`compile.{language}`)
3. Implement compilation logic
4. Publish results to `compilation.results`

No changes to API service required.

---

## Database Schema

### functions

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

CREATE INDEX idx_functions_status ON functions(status);
CREATE INDEX idx_functions_language ON functions(language);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, auto-generated | Unique identifier |
| `name` | VARCHAR(255) | NOT NULL | Human-readable name |
| `description` | TEXT | - | What the function does |
| `language` | VARCHAR(50) | NOT NULL | Source language (e.g., `assemblyscript`) |
| `source` | TEXT | NOT NULL | Original source code |
| `wasm_binary` | BYTEA | NULLABLE | Compiled WASM (null until compiled) |
| `status` | VARCHAR(20) | NOT NULL | `PENDING`, `COMPILING`, `READY`, `FAILED` |
| `compile_error` | TEXT | NULLABLE | Error message if compilation failed |
| `created_at` | TIMESTAMP | NOT NULL | When registered |
| `updated_at` | TIMESTAMP | NOT NULL | Last modification |

### executions

```sql
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

CREATE INDEX idx_executions_function_id ON executions(function_id);
CREATE INDEX idx_executions_status ON executions(status);
CREATE INDEX idx_executions_started_at ON executions(started_at DESC);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, auto-generated | Unique identifier |
| `function_id` | UUID | FK → functions, ON DELETE SET NULL | Which function was executed |
| `input` | JSONB | NOT NULL | Input data provided |
| `output` | JSONB | NULLABLE | Output data returned |
| `status` | VARCHAR(20) | NOT NULL | `PENDING`, `RUNNING`, `COMPLETED`, `FAILED` |
| `started_at` | TIMESTAMP | NOT NULL | Execution start |
| `completed_at` | TIMESTAMP | NULLABLE | Execution end |
| `metadata` | JSONB | NULLABLE | Error details, logs, timing |

---

## User Stories

### 1. Register a Function

**As a** developer  
**I want to** register a function with a name, language, and source code  
**So that** it can be compiled and executed later

**Acceptance Criteria:**
- [ ] POST `/functions` accepts `name`, `description`, `language`, and `source`
- [ ] Returns the created function with a generated `id` and `status: PENDING`
- [ ] `name` and `source` are required
- [ ] `language` must be a supported language (e.g., `assemblyscript`)
- [ ] Compilation job is published to RabbitMQ
- [ ] Function status updates to `COMPILING`, then `READY` or `FAILED`

---

### 2. List Available Functions

**As a** developer  
**I want to** list all registered functions  
**So that** I can see what functions are available to execute

**Acceptance Criteria:**
- [ ] GET `/functions` returns a list of all functions
- [ ] Each function includes `id`, `name`, `description`, `language`, `status`, `created_at`
- [ ] Returns empty list if no functions exist

---

### 3. Get Function Details

**As a** developer  
**I want to** retrieve details of a specific function  
**So that** I can see its configuration and status before executing

**Acceptance Criteria:**
- [ ] GET `/functions/{id}` returns the function details
- [ ] Returns 404 if function does not exist
- [ ] Response includes `id`, `name`, `description`, `language`, `source`, `status`, `compile_error`, `created_at`, `updated_at`
- [ ] Does NOT include `wasm_binary` (internal detail)

---

### 4. Update a Function

**As a** developer  
**I want to** update an existing function  
**So that** I can fix bugs or improve its implementation

**Acceptance Criteria:**
- [ ] PUT `/functions/{id}` accepts `name`, `description`, `language`, and/or `source`
- [ ] If `source` or `language` changes, triggers recompilation (status → `PENDING`)
- [ ] Returns the updated function
- [ ] `updated_at` is set automatically
- [ ] Returns 404 if function does not exist

---

### 5. Delete a Function

**As a** developer  
**I want to** delete a function  
**So that** I can remove deprecated or unused functions

**Acceptance Criteria:**
- [ ] DELETE `/functions/{id}` removes the function
- [ ] Returns 204 No Content on success
- [ ] Returns 404 if function does not exist
- [ ] Associated executions are preserved (`function_id` set to NULL)

---

### 6. Execute a Function

**As a** developer  
**I want to** execute a function with input data  
**So that** I can get computed results

**Acceptance Criteria:**
- [ ] POST `/functions/{id}/execute` accepts `input` (JSON)
- [ ] Returns 400 if function status is not `READY`
- [ ] Creates an Execution record with `status: PENDING`
- [ ] Loads WASM binary and executes via Chicory
- [ ] Returns execution result with `id`, `status`, `output`, `started_at`, `completed_at`
- [ ] Returns 404 if function does not exist
- [ ] Handles execution errors gracefully (`status: FAILED`, error in `metadata`)

---

### 7. Get Execution Details

**As a** developer  
**I want to** retrieve details of a specific execution  
**So that** I can inspect results or debug failures

**Acceptance Criteria:**
- [ ] GET `/executions/{id}` returns the execution details
- [ ] Returns 404 if execution does not exist
- [ ] Response includes `id`, `function_id`, `input`, `output`, `status`, `started_at`, `completed_at`, `metadata`

---

### 8. List Executions for a Function

**As a** developer  
**I want to** list all executions for a specific function  
**So that** I can see the execution history

**Acceptance Criteria:**
- [ ] GET `/functions/{id}/executions` returns list of executions
- [ ] Returns 404 if function does not exist
- [ ] Results are ordered by `started_at` descending (most recent first)
- [ ] Each execution includes `id`, `status`, `started_at`, `completed_at`

---

## API Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/functions` | Register a new function |
| GET | `/functions` | List all functions |
| GET | `/functions/{id}` | Get function details |
| PUT | `/functions/{id}` | Update a function |
| DELETE | `/functions/{id}` | Delete a function |
| POST | `/functions/{id}/execute` | Execute a function |
| GET | `/functions/{id}/executions` | List executions for a function |
| GET | `/executions/{id}` | Get execution details |

---

## Project Structure

```
projectNIL/
├── services/
│   ├── api-service/                        # Spring Boot
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/projectnil/api/
│   │       │   │   ├── ApiServiceApplication.java
│   │       │   │   ├── controller/
│   │       │   │   │   ├── FunctionController.java
│   │       │   │   │   └── ExecutionController.java
│   │       │   │   ├── service/
│   │       │   │   │   ├── FunctionService.java
│   │       │   │   │   └── ExecutionService.java
│   │       │   │   ├── repository/
│   │       │   │   │   ├── FunctionRepository.java
│   │       │   │   │   └── ExecutionRepository.java
│   │       │   │   ├── model/
│   │       │   │   │   ├── Function.java
│   │       │   │   │   └── Execution.java
│   │       │   │   ├── messaging/
│   │       │   │   │   ├── CompilationPublisher.java
│   │       │   │   │   └── CompilationResultListener.java
│   │       │   │   └── runtime/
│   │       │   │       └── WasmExecutor.java
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── db/changelog/
│   │       │           ├── db.changelog-master.yaml
│   │       │           └── migrations/
│   │       │               ├── 001-create-functions-table.sql
│   │       │               └── 002-create-executions-table.sql
│   │       └── test/
│   │
│   └── compiler-assemblyscript/            # Node.js
│       ├── package.json
│       ├── src/
│       │   ├── index.js
│       │   ├── compiler.js
│       │   └── messaging.js
│       └── Dockerfile
│
├── gradle/
│   └── libs.versions.toml
├── settings.gradle
├── build.gradle
├── compose.yml
└── docs/
```

---

## Decisions Made

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Function storage | WASM binary in PostgreSQL (BYTEA) | Simplicity, transactional consistency |
| Runtime | Chicory (pure Java) | No JNI, easy embedding |
| Compilation | Async via RabbitMQ | Decoupling, extensibility |
| First language | AssemblyScript | Easy toolchain, fast compilation |
| Execution model | Synchronous (Phase 0) | Simplicity; async execution in later phase |
| Pagination | Not implemented | Defer until needed |
| Service discovery | Not implemented | RabbitMQ provides decoupling |

See [ADR 001: WASM Runtime](./decisions/001-wasm-runtime.md) for detailed rationale on the WASM decision.

---

## Related Documents

- [Technology Stack](./stack.md) - Full technology choices and rationale
- [Domain Model](./domain.md) - Overall domain model (WIP)
- [ADR 001: WASM Runtime](./decisions/001-wasm-runtime.md) - Architecture decision record
