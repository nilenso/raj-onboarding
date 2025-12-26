# Practices (Canonical)

This doc records cross-cutting practices: retries, idempotency, observability, error handling, and testing.

## Idempotency

### Compilation Result Processing
Because pgmq can redeliver messages (visibility timeout), the API’s result-consumer MUST be idempotent:
- Applying the same `CompilationResult` twice must not corrupt state.

Recommended rules:
- If `success=true`:
  - Set `status=READY` and set/overwrite `wasmBinary`.
  - Clear `compileError`.
- If `success=false`:
  - Set `status=FAILED`.
  - Set/overwrite `compileError`.

### Execution Creation
- `POST /functions/{id}/execute` creates a new `Execution` every time; it is not idempotent.

## Retries and Failure Handling

### Compiler Service
- On **compilation failure** (user code invalid): publish `CompilationResult(success=false, error=...)` and then delete/archive the job message.
- On **transient infrastructure failure** (DB connectivity, disk full, process crash): allow job message to reappear (do not delete), so it can be retried.

### API Service
- When reading results, delete/archive only after successful persistence.

## Observability

### Correlation
- Use `functionId` as the compilation correlation identifier.
- Use `executionId` as the runtime correlation identifier.

### Recommended Logs
- API:
  - `function.created` (functionId, language)
  - `compilation.job.published` (functionId)
  - `compilation.result.applied` (functionId, success)
  - `execution.started` (executionId, functionId)
  - `execution.completed` / `execution.failed`
- Compiler:
  - `job.received` (functionId, language)
  - `job.compiled` (functionId, durationMs)
  - `job.failed` (functionId, errorType)
  - `result.published` (functionId)

## API Error Semantics (Convergence)

There are two viable conventions for runtime failures:
- **Option A**: Always return `200` with `ExecutionResponse.status=FAILED` (current docs imply this).
- **Option B**: Return `5xx` for runtime/platform failures and still persist an `Execution` record.

Canonical recommendation:
- Return `200` for *function* runtime failures (treated as a domain outcome).
- Return `5xx` only for platform failures where the execution could not be recorded.

## DTO Convergence: `ExecutionRequest.input`

Current code uses `ExecutionRequest(String input)` (stringified JSON), while docs show `input` as an object.

Canonical recommendation:
- Standardize on `input: object` in HTTP.
- API should persist the serialized JSON string into `executions.input`.

## Security (Phase 0)
- WASM provides sandboxing boundaries, but the platform still MUST:
  - enforce resource limits (time, memory) in the runtime
  - validate exported ABI (expected `handle` export)

## Testing
- Reference: `projectNIL/docs/testing-strategy.md`.
- Canonical test categories:
  - Unit: entity invariants, DTO mapping.
  - Integration: Postgres + Liquibase + pgmq publish/read.
  - E2E smoke: POST /functions → compile → READY → execute.
