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

### Not Yet Implemented

| Component | Location | Blocked By |
|-----------|----------|------------|
| FunctionController CRUD | `api/src/.../web/` | - |
| MessagePublisher impl | `api/src/.../queue/` | - |
| CompilationPoller | `api/src/.../queue/` | MessagePublisher |

### Endpoint Implementation Status

| Method | Endpoint | Issue | Status |
|--------|----------|-------|--------|
| POST | `/functions` | #24 | Not started |
| GET | `/functions` | #25 | Not started |
| GET | `/functions/{id}` | #26 | Not started |
| PUT | `/functions/{id}` | #27 | Not started |
| DELETE | `/functions/{id}` | #28 | Not started |
| POST | `/functions/{id}/execute` | #29 | **Done** |
| GET | `/functions/{id}/executions` | #30 | Not started |
| GET | `/executions/{id}` | #31 | Not started |

---

## 9. Next Steps (Recommended Order)

1. ~~**Database configuration** - Add datasource config to `application.yaml`~~ Done
2. ~~**Repositories** - Create `FunctionRepository` and `ExecutionRepository`~~ Done
3. **FunctionService CRUD** - Add create, update, delete operations
4. **FunctionController CRUD** - REST endpoints for function management
5. **MessagePublisher** - PGMQ integration for compilation jobs
6. **CompilationPoller** - Background polling for compilation results
7. **Execution queries** - `GET /executions/{id}` and `GET /functions/{id}/executions`

