# DESIGN: API Service Blueprint

Status: Active
Related: #44, #33, #37, #41, #50, #29

Canonical end-to-end spec lives under `projectNIL/scope/`.

## 1. Domain Entities (`com.projectnil.api.domain`)

### `Function`
- `UUID id` (Primary Key)
- `String name` (Unique)
- `String description`
- `String language` (e.g., "assemblyscript")
- `String source` (The raw source code)
- `byte[] wasmBinary` (The compiled module)
- `FunctionStatus status` (PENDING, COMPILING, READY, FAILED)
- `String compileError` (Logs if compilation fails)
- `LocalDateTime createdAt/updatedAt`

### `Execution`
- `UUID id`
- `UUID functionId` (FK)
- `String input` (JSON serialized from the HTTP object)
- `String output` (JSON serialized response)
- `ExecutionStatus status` (PENDING, RUNNING, COMPLETED, FAILED)
- `String errorMessage`
- `LocalDateTime startedAt/completedAt`

---

## 2. PGMQ Messaging (`com.projectnil.api.queue`)

These DTOs map to the JSON payload stored in PGMQ messages.

### `CompilationJob`
- `UUID functionId`
- `String language`
- `String source`

### `CompilationResult`
- `UUID functionId`
- `boolean success`
- `byte[] wasmBinary`
- `String error`

---

## 3. Web DTOs (`com.projectnil.api.web`)

### `FunctionRequest`
Used for `POST /functions`.
- `String name`
- `String description`
- `String language`
- `String source`

### `FunctionResponse`
- `UUID id`
- `String name`
- `FunctionStatus status`
- `LocalDateTime createdAt`

---

## 4. Key Interfaces & Components

### `runtime.WasmRuntime` (Interface)
Abstraction over the WASM engine.
```java
public interface WasmRuntime {
    byte[] execute(byte[] wasmBinary, String inputJson) throws Exception;
}
```

### `runtime.ChicoryWasmRuntime` (Implementation)
Actual implementation using Chicory 1.6.1. Key features:
- Parses WASM binary and instantiates module with host functions
- Provides `env.abort` host function required by AssemblyScript
- Validates `handle` export and AssemblyScript runtime exports
- Uses `WasmStringCodec` for language-specific string I/O
- Enforces configurable timeout (default 10s) via `ExecutorService`
- Logs warnings for high memory usage (>16MB)

### `runtime.WasmStringCodec` (Interface)
Abstracts language-specific string memory handling:
```java
public interface WasmStringCodec {
    void validateExports(Instance instance) throws WasmAbiException;
    int writeString(Instance instance, String value);
    String readString(Instance instance, int pointer);
    void cleanup(Instance instance, int pointer);
}
```

### `runtime.AssemblyScriptStringCodec` (Implementation)
Handles AssemblyScript's UTF-16LE encoding and GC-managed memory:
- Uses `__new`, `__pin`, `__unpin` for memory management
- Reads string length from `rtSize` field at `pointer - 4`
- Converts between Java String (UTF-16) and AS memory layout

### `runtime.WasmExecutionException`
Runtime errors: traps, timeouts, invalid output.

### `runtime.WasmAbiException`
ABI violations: missing exports, wrong signatures.

### `queue.MessagePublisher` (Interface)
Generic interface for sending messages to queues.
```java
public interface MessagePublisher<T> {
    void publish(String queueName, T message);
}
```

### `queue.CompilationPoller` (Component)
Background service using Java 25 Virtual Threads to poll PGMQ for results.
```java
@Component
public class CompilationPoller {
    // Polls 'compilation_results' queue every 5 seconds
    // Executes on a dedicated virtual thread
}
```

---

## 5. Compile State Machine

1. **Client** calls `POST /functions`.
2. **API Controller**:
   - Saves `Function` with status `PENDING`.
   - Maps `Function` to `CompilationJob`.
   - Calls `MessagePublisher.publish("compilation_jobs", job)`.
3. **Compiler Service** (External JVM worker):
   - Pulls from `compilation_jobs`.
   - Compiles AssemblyScript to WASM.
   - Publishes `CompilationResult` to `compilation_results`.
4. **API Service** (CompilationPoller):
   - Polls `compilation_results` queue.
   - Updates `Function` with WASM binary and status `READY`, or error and status `FAILED`.

```
PENDING ──(job published)──> COMPILING ──(success)──> READY
                                │
                                └──(failure)──> FAILED
```

---

## 6. Execute State Machine

1. **Client** calls `POST /functions/{id}/execute`.
2. **API Controller**:
   - Validates `Function.status == READY` (rejects with 400 if not).
   - Creates `Execution` with status `PENDING`.
   - Invokes `WasmRuntime.execute(wasmBinary, inputJson)`.
3. **WasmRuntime**:
   - Parses WASM, instantiates module with host functions.
   - Writes input string to memory, calls `handle()`.
   - Reads output string from memory, returns result.
4. **API Controller**:
   - On success: Updates `Execution` status to `COMPLETED`, stores output.
   - On failure: Updates `Execution` status to `FAILED`, stores error message.

```
PENDING ──(start)──> RUNNING ──(success)──> COMPLETED
                        │
                        └──(failure)──> FAILED
```

---

## 7. Technical Nuances & Decisions

### Persistence Configuration (Spring Boot 4.0.0+)
In this specific environment, entity scanning and JPA repository configuration moved from `org.springframework.boot.autoconfigure.domain` to `org.springframework.boot.persistence.autoconfigure`.
- Use `@EntityScan("com.projectnil.common.domain")` on `ApiApplication`.

### Records for DTOs
All Web and Queue DTOs are implemented as Java `records`. This enforces immutability (Effective Java Item 17) and provides a clean separation from mutable JPA entities.

### Lombok Configuration
- Star imports are prohibited by Checkstyle.
- `@Builder.Default` is required alongside field initialization for default values (e.g., `FunctionStatus.PENDING`) to work within the builder pattern.

---

## 8. Implementation Status

### Completed

| Component | Location | Status |
|-----------|----------|--------|
| Domain entities | `common/src/.../domain/` | Done |
| Queue DTOs | `common/src/.../domain/queue/` | Done |
| Web DTOs | `api/src/.../web/` | Done |
| WASM Runtime | `api/src/.../runtime/` | Done (PR #37) |
| Health endpoint | `api/src/.../web/health/` | Done |
| Database configuration | `api/src/.../resources/application.yaml` | Done (#29) |
| FunctionRepository | `api/src/.../repository/` | Done (#29) |
| ExecutionRepository | `api/src/.../repository/` | Done (#29) |
| FunctionService | `api/src/.../service/` | Done (#29) |
| ExecutionService | `api/src/.../service/` | Done (#29) |
| FunctionController | `api/src/.../web/` | Partial (#29 - execute only) |
| Global exception handler | `api/src/.../web/` | Done (#29) |

### Completed (This Session - #53, #54)

| Component | Location | Status |
|-----------|----------|--------|
| PgmqClient interface | `api/src/.../messaging/` | Done (#54) |
| JdbcPgmqClient | `api/src/.../messaging/` | Done (#54) |
| PgmqProperties | `api/src/.../messaging/` | Done (#54) |
| PgmqConfiguration | `api/src/.../messaging/` | Done (#54) |
| CompilationResultPoller | `api/src/.../messaging/` | Done (#53) |
| CompilationResultHandler | `api/src/.../service/` | Done (#53) |
| FunctionController (CRUD) | `api/src/.../web/` | Done (#54) |

### Not Yet Implemented

| Component | Location | Blocked By |
|-----------|----------|------------|
| PUT /functions/{id} | `api/src/.../web/` | - |

### Endpoint Implementation Status

| Method | Endpoint | Issue | Status |
|--------|----------|-------|--------|
| POST | `/functions` | #24 | **Done** (#54) |
| GET | `/functions` | #25 | **Done** (#54) |
| GET | `/functions/{id}` | #26 | **Done** (#54) |
| PUT | `/functions/{id}` | #27 | **Done** |
| DELETE | `/functions/{id}` | #28 | **Done** (#54) |
| POST | `/functions/{id}/execute` | #29 | **Done** |
| GET | `/functions/{id}/executions` | #31 | **Done** |
| GET | `/executions/{id}` | #30 | **Done** |

---

## 9. Queue Integration Roadmap (#53, #54)

This section details the implementation plan for PGMQ integration in the API service.

### 9.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Service                                     │
│  ┌─────────────────────┐     ┌─────────────────────────────────────────┐    │
│  │  FunctionController │     │  CompilationResultPoller                │    │
│  │  POST /functions    │     │  @Scheduled (background thread)         │    │
│  └─────────┬───────────┘     └───────────────────┬─────────────────────┘    │
│            │                                      │                          │
│            ▼                                      ▼                          │
│  ┌─────────────────────┐     ┌─────────────────────────────────────────┐    │
│  │  FunctionService    │     │  CompilationResultHandler               │    │
│  │  - create()         │     │  - applyResult(CompilationResult)       │    │
│  │  - update()         │     │  - idempotent updates                   │    │
│  └─────────┬───────────┘     └───────────────────┬─────────────────────┘    │
│            │                                      │                          │
│            ▼                                      ▼                          │
│  ┌─────────────────────┐     ┌─────────────────────────────────────────┐    │
│  │  CompilationJob     │     │  FunctionRepository                     │    │
│  │  Publisher          │     │  - save(), findById()                   │    │
│  └─────────┬───────────┘     └─────────────────────────────────────────┘    │
│            │                                                                 │
└────────────┼─────────────────────────────────────────────────────────────────┘
             │                              ▲
             ▼                              │
┌────────────────────────────┐   ┌──────────────────────────────┐
│  pgmq: compilation_jobs    │   │  pgmq: compilation_results   │
└────────────────────────────┘   └──────────────────────────────┘
             │                              ▲
             ▼                              │
┌─────────────────────────────────────────────────────────────────┐
│                        Compiler Service                          │
│  (Already implemented - polls jobs, publishes results)           │
└─────────────────────────────────────────────────────────────────┘
```

### 9.2 Components to Implement

| Component | Package | Responsibility |
|-----------|---------|----------------|
| `PgmqClient` | `api.messaging` | Interface for PGMQ operations |
| `JdbcPgmqClient` | `api.messaging` | JDBC-based PGMQ implementation |
| `CompilationJobPublisher` | `api.messaging` | Publishes `CompilationJob` to queue |
| `CompilationResultPoller` | `api.messaging` | Polls `compilation_results` queue |
| `CompilationResultHandler` | `api.service` | Applies results to function (idempotent) |

### 9.3 Implementation Details

#### 9.3.1 Issue #54: Publish Compilation Jobs

**Flow** (per `scope/flows.md` Flow 1):
```
POST /functions
    │
    ├─ Validate FunctionRequest (name, language, source)
    ├─ Validate language in SUPPORTED_LANGUAGES (Phase 0: "assemblyscript")
    ├─ Create Function(status=PENDING)
    ├─ Save to database
    ├─ Publish CompilationJob to queue
    ├─ Log "compilation.job.published"
    └─ Return 201 FunctionResponse(status=PENDING)
```

**Key decisions**:
- Function stays `PENDING` until compiler picks it up (compiler sets `COMPILING`)
- Job publication failure → rollback transaction, return 500
- Use same transaction for DB + queue publish (queue is backed by Postgres)

#### 9.3.2 Issue #53: Consume Compilation Results

**Flow** (per `scope/flows.md` Flow 1 & 2):
```
CompilationResultPoller (@Scheduled every 1s)
    │
    ├─ Read message from compilation_results (visibility timeout: 30s)
    ├─ Parse CompilationResult(functionId, success, wasmBinary, error)
    ├─ Call CompilationResultHandler.applyResult()
    │       ├─ Find function by ID (skip if not found)
    │       ├─ If success=true:
    │       │   ├─ Decode base64 wasmBinary
    │       │   ├─ Set status=READY, wasmBinary, compileError=null
    │       │   └─ Save
    │       ├─ If success=false:
    │       │   ├─ Set status=FAILED, wasmBinary=null, compileError=error
    │       │   └─ Save
    │       └─ Log "compilation.result.applied"
    ├─ Delete/archive message from queue
    └─ Repeat
```

**Idempotency** (per `scope/practices.md`):
- Re-applying the same result must not corrupt state
- If function already `READY` or `FAILED`, log warning and skip
- Use transaction: DB update + message delete atomic

### 9.4 Configuration Properties

```yaml
projectnil:
  pgmq:
    job-queue: compilation_jobs
    result-queue: compilation_results
    poll-interval-ms: 1000
    visibility-timeout-seconds: 30
```

### 9.5 Error Handling

| Scenario | Behavior |
|----------|----------|
| Job publish fails | Transaction rollback, 500 to client |
| Result poll fails | Log error, continue polling |
| Function not found on result | Log warning, delete message |
| DB update fails | Don't delete message (redelivery) |

### 9.6 Testing Strategy

- **Unit tests**: Mock `JdbcTemplate` for `JdbcPgmqClient`
- **Integration tests**: Testcontainers with PostgreSQL + PGMQ extension
- **End-to-end**: Register function → Compiler processes → Result applied

---

## 10. Update Function (#27)

This section details the implementation for `PUT /functions/{id}`.

### 10.1 Flow

Per `scope/contracts.md` and issue #27 acceptance criteria:

```
PUT /functions/{id}
    │
    ├─ Find function by ID (404 if not found)
    ├─ Validate FunctionRequest (name, language, source)
    ├─ Validate language in SUPPORTED_LANGUAGES
    ├─ Check if recompilation is needed:
    │   └─ If source OR language changed:
    │       ├─ Reset status to PENDING
    │       ├─ Clear wasmBinary and compileError
    │       ├─ Publish CompilationJob to queue
    │       └─ Log "function.recompilation.triggered"
    ├─ Update fields (name, description, language, source)
    ├─ updatedAt refreshed automatically by JPA
    ├─ Save to database
    └─ Return 200 FunctionResponse (expanded view)
```

### 10.2 Recompilation Logic

| Old State | source changed | language changed | Action |
|-----------|----------------|------------------|--------|
| READY     | Yes            | -                | Reset to PENDING, clear wasm, publish job |
| READY     | -              | Yes              | Reset to PENDING, clear wasm, publish job |
| PENDING   | Yes            | -                | Reset to PENDING, publish new job |
| COMPILING | Yes            | -                | Reset to PENDING, publish new job (supersedes) |
| FAILED    | Yes            | -                | Reset to PENDING, clear error, publish job |
| Any       | No             | No               | Just update name/description |

### 10.3 Implementation

**FunctionService.update()**:
```java
@Transactional
public FunctionResponse update(UUID id, FunctionRequest request) {
    Function function = findById(id);
    validateLanguage(request.language());
    
    boolean needsRecompile = !Objects.equals(function.getSource(), request.source())
            || !Objects.equals(function.getLanguage(), request.language());
    
    function.setName(request.name());
    function.setDescription(request.description());
    function.setLanguage(request.language());
    function.setSource(request.source());
    
    if (needsRecompile) {
        function.setStatus(FunctionStatus.PENDING);
        function.setWasmBinary(null);
        function.setCompileError(null);
        
        CompilationJob job = new CompilationJob(
                function.getId(),
                function.getLanguage(),
                function.getSource()
        );
        pgmqClient.publishJob(job);
        LOG.info("function.recompilation.triggered id={}", id);
    }
    
    function = functionRepository.save(function);
    LOG.info("function.updated id={} needsRecompile={}", id, needsRecompile);
    
    return toExpandedResponse(function);
}
```

**FunctionController.update()**:
```java
@PutMapping("/{functionId}")
public ResponseEntity<FunctionResponse> update(
        @PathVariable UUID functionId,
        @RequestBody FunctionRequest request) {
    FunctionResponse response = functionService.update(functionId, request);
    return ResponseEntity.ok(response);
}
```

### 10.4 Response Format

Per `scope/contracts.md`, update returns expanded view:
```json
{
  "id": "...",
  "name": "add",
  "description": "Adds two numbers",
  "language": "assemblyscript",
  "source": "...",
  "status": "PENDING",
  "compileError": null,
  "createdAt": "2025-12-18T10:00:00Z",
  "updatedAt": "2025-12-27T10:00:00Z"
}
```

### 10.5 Error Handling

| Scenario | HTTP Response |
|----------|---------------|
| Function not found | 404 Not Found |
| Unsupported language | 415 Unsupported Media Type |
| Invalid request body | 400 Bad Request |

---

## 11. Executions API (#30, #31)

This section details the implementation for execution query endpoints.

### 11.1 GET /executions/{id} (#30)

Returns detailed execution record for debugging and inspection.

**Flow**:
```
GET /executions/{id}
    │
    ├─ Find execution by ID (404 if not found)
    ├─ Build ExecutionDetailResponse with all fields
    │   ├─ id, functionId, status
    │   ├─ input, output (JSON strings)
    │   ├─ errorMessage (only if FAILED)
    │   ├─ startedAt, completedAt, createdAt
    └─ Return 200 ExecutionDetailResponse
```

**Response DTO**: `ExecutionDetailResponse`
```json
{
  "id": "...",
  "functionId": "...",
  "status": "COMPLETED",
  "input": "{\"a\": 5, \"b\": 3}",
  "output": "{\"result\": 8}",
  "errorMessage": null,
  "startedAt": "2025-12-27T10:00:00Z",
  "completedAt": "2025-12-27T10:00:01Z",
  "createdAt": "2025-12-27T10:00:00Z"
}
```

### 11.2 GET /functions/{id}/executions (#31)

Returns lightweight execution history for a function.

**Flow**:
```
GET /functions/{id}/executions
    │
    ├─ Validate function exists (404 if not found)
    ├─ Query executions by functionId, ordered by startedAt DESC
    ├─ Map to ExecutionSummaryResponse (excludes input/output)
    └─ Return 200 List<ExecutionSummaryResponse>
```

**Response DTO**: `ExecutionSummaryResponse` (lightweight)
```json
{
  "id": "...",
  "status": "COMPLETED",
  "startedAt": "2025-12-27T10:00:00Z",
  "completedAt": "2025-12-27T10:00:01Z"
}
```

Per issue #31, heavy fields (`input`, `output`) are excluded from list response.

### 11.3 Implementation

**ExecutionController** (new):
```java
@RestController
@RequestMapping("/executions")
public class ExecutionController {
    
    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionDetailResponse> get(@PathVariable UUID executionId) {
        return ResponseEntity.ok(executionService.findById(executionId));
    }
}
```

**FunctionController** (addition):
```java
@GetMapping("/{functionId}/executions")
public ResponseEntity<List<ExecutionSummaryResponse>> listExecutions(
        @PathVariable UUID functionId) {
    return ResponseEntity.ok(executionService.findByFunctionId(functionId));
}
```

### 11.4 Error Handling

| Scenario | HTTP Response |
|----------|---------------|
| Execution not found | 404 Not Found |
| Function not found | 404 Not Found |
| Empty execution list | 200 with `[]` |

---

## 12. Canonical DTO Alignment (#55)

Per `scope/contracts.md`, this section details DTO serialization alignment.

### 12.1 Changes

| DTO | Field | Before | After |
|-----|-------|--------|-------|
| `ExecutionResponse` | `output` | `String` | `Object` (parsed JSON) |
| `ExecutionDetailResponse` | `input` | `String` | `Object` (parsed JSON) |
| `ExecutionDetailResponse` | `output` | `String` | `Object` (parsed JSON) |
| `ExecutionRequest` | `input` | `Object` (any) | `Object` (validated as Map) |

### 12.2 Input Validation

Per `scope/contracts.md`, `ExecutionRequest.input` must be a JSON object:

```java
private String serializeInput(Object input) {
    if (input == null) {
        return "{}";
    }
    if (!(input instanceof java.util.Map)) {
        throw new InvalidInputException(
                "Input must be a JSON object, got: " + input.getClass().getSimpleName());
    }
    return objectMapper.writeValueAsString(input);
}
```

### 12.3 Output Parsing

Output is parsed from JSON string to Object for response:

```java
private Object parseOutput(String outputJson) {
    if (outputJson == null || outputJson.isBlank()) {
        return null;
    }
    return objectMapper.readValue(outputJson, Object.class);
}
```

### 12.4 Error Handling

| Scenario | HTTP Response |
|----------|---------------|
| Input is primitive (string/number) | 400 Bad Request |
| Input is array | 400 Bad Request |
| Input is null | Treated as `{}` |

---

## 13. Next Steps (Recommended Order)

1. ~~**Database configuration** - Add datasource config to `application.yaml`~~ Done
2. ~~**Repositories** - Create `FunctionRepository` and `ExecutionRepository`~~ Done
3. ~~**Queue Integration** (#54, #53) - PGMQ publisher and poller~~ Done
4. ~~**FunctionService CRUD** - Add create, update, delete operations~~ Done
5. ~~**FunctionController CRUD** - REST endpoints for function management~~ Done
6. ~~**PUT /functions/{id}** - Update function and recompile~~ Done (#27)
7. ~~**Execution queries** - `GET /executions/{id}` and `GET /functions/{id}/executions`~~ Done (#30, #31)
8. ~~**DTO Alignment** - Canonical serialization per scope/contracts.md~~ Done (#55)

