# Contracts (Canonical)

This doc specifies all external and cross-service contracts:
- HTTP API (Client ↔ API service)
- Queue messages (API ↔ compiler service)
- WASM ABI expectations (API ↔ compiled modules)

## HTTP API

Base URL: `http://<host>:8080`
Content-Type: `application/json`

### Endpoints
- `POST /functions`
- `GET /functions`
- `GET /functions/{id}`
- `PUT /functions/{id}`
- `DELETE /functions/{id}`
- `POST /functions/{id}/execute`
- `GET /functions/{id}/executions`
- `GET /executions/{id}`

### DTO: `FunctionRequest` (create/update)
```json
{
  "name": "add",
  "description": "Adds two numbers",
  "language": "assemblyscript",
  "source": "export function add(a: i32, b: i32): i32 { return a + b; }"
}
```

### DTO: `FunctionResponse` (minimal)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add",
  "status": "PENDING",
  "createdAt": "2025-12-18T10:00:00Z"
}
```

### DTO: Function Details (expanded)
The platform SHOULD expose the expanded view (even if `FunctionResponse` remains minimal):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add",
  "description": "Adds two numbers",
  "language": "assemblyscript",
  "source": "...",
  "status": "READY",
  "compileError": null,
  "createdAt": "2025-12-18T10:00:00Z",
  "updatedAt": "2025-12-18T10:00:05Z"
}
```

### DTO: `ExecutionRequest`
Canonical contract:
```json
{ "input": { "a": 5, "b": 3 } }
```

The API serializes the JSON object to store it in `executions.input`.

### DTO: `ExecutionResponse`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "output": { "result": 8 },
  "errorMessage": null,
  "createdAt": "2025-12-18T10:01:00Z"
}
```

### Error Semantics
- `400 Bad Request`
  - Invalid DTO payload
  - Attempt to execute a non-`READY` function
- `404 Not Found`
  - Unknown function/execution ID
- `500 Internal Server Error`
  - Unexpected platform failure

## Queue Contracts (pgmq)

Queues:
- `compilation_jobs`
- `compilation_results`

### Message: `CompilationJob` (API → compiler)
```json
{
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "language": "assemblyscript",
  "source": "export function add(a: i32, b: i32): i32 { return a + b; }"
}
```

### Message: `CompilationResult` (compiler → API)
```json
{
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "success": true,
  "wasmBinary": "AGFzbQEAAAA...",
  "error": null
}
```

Encoding rule:
- `wasmBinary` MUST be base64-encoded in JSON.
- API MUST decode base64 into a `byte[]` for persistence.

## WASM ABI Contract (Canonical)

Because languages differ in calling conventions, ProjectNIL MUST standardize an ABI.

### Canonical ABI (Phase 0)
- The compiled module MUST export a function named `handle`.
- Signature MUST be logically: `handle(inputJson: string) -> string`.
- `inputJson` and output strings are UTF-8 at the API boundary.

This keeps the API runtime generic across languages.

### AssemblyScript-Specific ABI

AssemblyScript modules compiled with `--exportRuntime` MUST export:

| Export | WASM Signature | Purpose |
|--------|----------------|---------|
| `handle` | `(i32) -> i32` | User function (string pointers) |
| `__new` | `(i32, i32) -> i32` | Allocate managed object |
| `__pin` | `(i32) -> i32` | Pin object (prevent GC) |
| `__unpin` | `(i32) -> void` | Unpin object (allow GC) |
| `memory` | (memory export) | Linear memory |

**String Memory Layout**:
```
┌────────────────────────────────┬─────────────────────────────┐
│  20-byte header (GC metadata)  │  UTF-16LE payload           │
├────────────────────────────────┼─────────────────────────────┤
│  ... | rtId (4B) | rtSize (4B) │  string data (rtSize bytes) │
└────────────────────────────────┴─────────────────────────────┘
         offset -8    offset -4    offset 0 (pointer)
```

- `rtId = 2` for strings
- `rtSize` = byte length of UTF-16LE payload

**Required Host Import**:
- `env.abort(messagePtr: i32, fileNamePtr: i32, line: i32, column: i32) -> void`
  - Called by AssemblyScript runtime on assertion failures
  - The API runtime provides this as a host function that throws `WasmExecutionException`

### Compilation Requirements

AssemblyScript source MUST be compiled with:
```bash
asc source.ts --outFile module.wasm --exportRuntime --runtime incremental --optimize
```

### Runtime Interface
The API's internal runtime contract is represented as:
- `com.projectnil.api.runtime.WasmRuntime.execute(wasmBinary, inputJson) -> byte[]`

The returned bytes SHOULD represent UTF-8 JSON.

### Error Handling

| Error Type | HTTP Response | Notes |
|------------|---------------|-------|
| WASM trap (unreachable, div by zero) | 200 with `status=FAILED` | User code error |
| Missing `handle` export | 500 | ABI violation |
| Execution timeout | 200 with `status=FAILED` | Resource limit |
| Platform failure | 500 | Infrastructure error |
