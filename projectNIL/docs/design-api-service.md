# DESIGN: API Service Blueprint

Status: Draft
Related: #44, #33, #37, #41, #50

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
- `String input` (JSON string)
- `String output` (JSON string)
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

### `runtime.ChicoryRuntime` (Implementation)
Actual implementation using Chicory 1.6.1. Handles memory allocation and pointer management.

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
3. **Compiler Service** (External Node.js):
   - Pulls from `compilation_jobs`.
   - Compiles and creates binary.
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

