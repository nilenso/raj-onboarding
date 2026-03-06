# System Flows

This document describes end-to-end flows across services, including success and failure paths.

## Legend

- **API** = API Service
- **DB** = PostgreSQL tables
- **Q** = pgmq queues
- **CS** = Compiler Service
- **WR** = WASM Runtime

---

## Flow 1: Register Function (Compilation Succeeds)

```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant API as API Service
  participant DB as Postgres (tables)
  participant QJ as pgmq: compilation_jobs
  participant CS as Compiler Service
  participant QR as pgmq: compilation_results

  C->>API: POST /functions (FunctionRequest)
  API->>DB: INSERT functions(status=PENDING, source, ...)
  API->>QJ: send CompilationJob(functionId, language, source)
  API-->>C: 201 FunctionResponse(status=PENDING)

  CS->>QJ: read CompilationJob
  CS->>DB: mark function status COMPILING
  CS->>CS: compile source -> wasmBinary
  CS->>QR: send CompilationResult(success=true, wasmBinary, error=null)

  API->>QR: read CompilationResult
  API->>DB: UPDATE functions(status=READY, wasm_binary, compile_error=null)
```

---

## Flow 2: Register Function (Compilation Fails)

```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant API as API Service
  participant DB as Postgres (tables)
  participant QJ as pgmq: compilation_jobs
  participant CS as Compiler Service
  participant QR as pgmq: compilation_results

  C->>API: POST /functions
  API->>DB: INSERT functions(status=PENDING)
  API->>QJ: send CompilationJob
  API-->>C: 201 FunctionResponse(status=PENDING)

  CS->>QJ: read CompilationJob
  CS->>DB: mark function status COMPILING
  CS->>CS: compile -> error (syntax/type)
  CS->>QR: send CompilationResult(success=false, wasmBinary=null, error=stderr)

  API->>QR: read CompilationResult
  API->>DB: UPDATE functions(status=FAILED, compile_error=error)
```

---

## Flow 3: Execute Function (Success)

**Precondition**: `Function.status == READY` and `Function.wasmBinary != null`

```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant API as API Service
  participant DB as Postgres (tables)
  participant WR as WasmRuntime

  C->>API: POST /functions/{id}/execute (ExecutionRequest)
  API->>DB: INSERT executions(status=RUNNING, input, started_at)
  API->>DB: SELECT functions.wasm_binary by id
  API->>WR: execute(wasmBinary, inputJson)
  WR-->>API: outputJson bytes
  API->>DB: UPDATE executions(status=COMPLETED, output, completed_at)
  API-->>C: 200 ExecutionResponse(status=COMPLETED, output)
```

---

## Flow 4: Execute Function (Rejected - Not Ready)

```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant API as API Service
  participant DB as Postgres (tables)

  C->>API: POST /functions/{id}/execute
  API->>DB: SELECT functions.status by id
  API-->>C: 400 Bad Request (cannot execute non-READY function)
```

---

## Flow 5: Execute Function (Runtime Failure)

Examples of runtime failures:
- WASM trap (e.g., unreachable instruction)
- Invalid ABI export
- Invalid JSON encoding in output

```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant API as API Service
  participant DB as Postgres (tables)
  participant WR as WasmRuntime

  C->>API: POST /functions/{id}/execute
  API->>DB: INSERT executions(status=RUNNING)
  API->>WR: execute(wasmBinary, inputJson)
  WR-->>API: throws error / trap
  API->>DB: UPDATE executions(status=FAILED, error_message)
  API-->>C: 200 ExecutionResponse(status=FAILED, errorMessage)
```

**Note**: Runtime failures return HTTP `200` because the platform operated correctly; the failure is in the user's function.

---

## Flow 6: Queue Redelivery / Retries

This flow describes message reprocessing when persistence fails.

```mermaid
sequenceDiagram
  autonumber
  participant API as API Service
  participant QR as pgmq: compilation_results
  participant DB as Postgres (tables)

  API->>QR: read CompilationResult (visibility timeout starts)
  API->>DB: UPDATE functions ...
  alt DB update succeeds
    API->>QR: delete/archive message
  else DB update fails (transient)
    Note over QR: message becomes visible again after timeout
    Note over API: must be idempotent on reprocessing
  end
```

---

## WASM Runtime Execution Details

### High-Level Flow

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Java Host      │     │  WASM Linear     │     │  AS Function    │
│  (API Service)  │     │  Memory          │     │  (handle)       │
└────────┬────────┘     └────────┬─────────┘     └────────┬────────┘
         │                       │                        │
         │ 1. Write JSON input   │                        │
         │──────────────────────>│                        │
         │                       │                        │
         │ 2. Call handle(ptr)   │                        │
         │───────────────────────────────────────────────>│
         │                       │                        │
         │                       │    3. Process & return │
         │<───────────────────────────────────────────────│
         │                       │                        │
         │ 4. Read JSON output   │                        │
         │<──────────────────────│                        │
```

### AssemblyScript Memory Model

AssemblyScript compiles `handle(string): string` to WASM as `handle(i32) -> i32` where arguments are pointers to memory.

**String Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│  20-byte header (GC metadata)  │  UTF-16LE payload          │
├────────────────────────────────┼─────────────────────────────┤
│  ... | rtId (4B) | rtSize (4B) │  string data (rtSize bytes) │
└────────────────────────────────┴─────────────────────────────┘
         offset -8    offset -4    offset 0 (pointer location)
```

### Required Module Exports

AssemblyScript modules compiled with `--exportRuntime` must export:

| Export | Signature | Purpose |
|--------|-----------|---------|
| `__new` | `(size: i32, id: i32) -> i32` | Allocate managed object |
| `__pin` | `(ptr: i32) -> i32` | Pin object (prevent GC) |
| `__unpin` | `(ptr: i32) -> void` | Unpin object (allow GC) |
| `handle` | `(ptr: i32) -> i32` | User function |
| `memory` | (export) | Linear memory |

### Memory Management Flow

1. **Allocate**: Call `__new(byteLength, STRING_CLASS_ID)` to allocate
2. **Pin**: Call `__pin(ptr)` to prevent GC during execution
3. **Write**: Copy UTF-16LE bytes to memory at pointer
4. **Execute**: Call `handle(inputPtr)` → get `outputPtr`
5. **Read**: Read `rtSize` from `outputPtr - 4`, read UTF-16LE bytes
6. **Cleanup**: Call `__unpin(inputPtr)` to allow GC

---

## Compiler Service Flow

### Runtime Flow

1. **Startup**: Load configuration, establish pgmq connection, verify queues exist
2. **Job Consumption**: Poll `compilation_jobs` with visibility timeout
3. **Language Filtering**: Process only jobs matching supported language; archive others
4. **Compilation**: 
   - Write source to temp file
   - Execute `asc <file> --binaryFile <output>.wasm --exportRuntime --runtime incremental --optimize`
   - Capture stdout/stderr
5. **Result Publishing**:
   - Success: Base64-encode WASM, publish with `success=true`
   - Failure: Publish with `success=false` and error message
6. **Cleanup**: Remove temp files, delete/archive queue message

### Error Handling

| Error Type | Action |
|------------|--------|
| Compilation failure (user code) | Publish `success=false`, archive message |
| Transient infrastructure failure | Allow message to reappear via visibility timeout |
| Fatal error (DB unavailable) | Exit process, let orchestrator restart |
