# End-to-End Flows (Canonical)

This doc describes end-to-end flows across services, including success and failure paths.

## Legend
- API = API service
- DB = Postgres tables
- Q = pgmq queues

## Flow 1: Register Function (Happy Path: compile succeeds)

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
  CS->>CS: compile source -> wasmBinary
  CS->>QR: send CompilationResult(success=true, wasmBinary, error=null)

  API->>QR: read CompilationResult
  API->>DB: UPDATE functions(status=READY, wasm_binary, compile_error=null)
```

## Flow 2: Register Function (Compile fails)

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
  CS->>CS: compile -> error (syntax/type)
  CS->>QR: send CompilationResult(success=false, wasmBinary=null, error=stderr)

  API->>QR: read CompilationResult
  API->>DB: UPDATE functions(status=FAILED, compile_error=error)
```

## Flow 3: Execute Function (Happy Path)

Precondition: `Function.status == READY` and `Function.wasmBinary != null`.

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

## Flow 4: Execute Function (Rejected: function not READY)

```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant API as API Service
  participant DB as Postgres (tables)

  C->>API: POST /functions/{id}/execute
  API->>DB: SELECT functions.status by id
  alt status != READY
    API-->>C: 400 Bad Request (cannot execute non-READY)
  else READY
    API-->>C: 200 (delegates to Flow 3)
  end
```

## Flow 5: Execute Function (Runtime failure)

Examples:
- WASM trap
- invalid ABI export
- invalid JSON encoding

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

## Flow 6: Queue Redelivery / Retries (Non-successful runs)

This flow describes what happens when processing fails mid-way.

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

## Observed Gaps to Converge (tracked in `scope/practices.md`)
- Define whether `execute` response for failures is `200` with `status=FAILED` vs an HTTP error.
- Decide if the API sets `Function.status=COMPILING` immediately after publishing job.
- Decide canonical shape of `ExecutionRequest.input` (JSON object vs JSON string).
