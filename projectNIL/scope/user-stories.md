# User Stories (Canonical)

These user stories drive the end-to-end flows in `scope/flows.md`.

## Phase 0 (Core FaaS)

### US-1: Register a Function
As a developer, I want to register a function by sending source code, so that the platform can compile it and make it executable.

Acceptance notes:
- The API returns a `functionId` immediately.
- The function becomes `READY` when compilation succeeds or `FAILED` when it fails.

### US-2: Observe Compilation Outcome
As a developer, I want to fetch function details, so that I can see whether compilation succeeded and view compile errors if it failed.

Acceptance notes:
- `GET /functions/{id}` shows `status` and `compileError`.

### US-3: Execute a Function
As a developer, I want to execute a compiled function with JSON input, so that I can get JSON output.

Acceptance notes:
- Execution only allowed when function is `READY`.
- Each execution is persisted with status and timestamps.

### US-4: Inspect Execution History
As a developer, I want to see past executions of a function, so that I can debug and verify behavior.

Acceptance notes:
- `GET /functions/{id}/executions` lists executions.
- `GET /executions/{id}` shows details.

## Operational Stories (Phase 0)

### US-O1: Debug a Stuck Compilation
As an operator, I want to see queue backlogs and correlate them to functions, so that I can detect stuck compiler workers.

Acceptance notes:
- Queues are inspectable in Postgres.
- Logs include `functionId`.

## Future (Non-Canonical for Phase 0)
The `projectNIL/docs/roadmap.md` describes Phase 1+ (auth, permissions, analytics). Once those are finalized, they can be promoted here.
