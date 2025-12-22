# DESIGN: API Service Blueprint

Status: Draft
Related: #44, #33, #37, #41

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
   - Pushes `CompilationResult` to `compilation_results`.
4. **CompilationPoller**:
   - Detects new message in `compilation_results`.
   - Updates `Function` entity in DB with `wasmBinary` and sets status to `READY`.
   - Acknowledges/Deletes message from PGMQ.

---

## 6. Project Configuration

### Virtual Threads (Java 25)
Enabling in `application.yml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
This ensures Tomcat and all `@Async` tasks run on lightweight virtual threads.
