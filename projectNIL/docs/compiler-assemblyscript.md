# AssemblyScript Compiler Service

## 1. Purpose
The AssemblyScript compiler service is the reference implementation of the ProjectNIL Compiler Interface. It consumes `CompilationJob` messages coming from the API service, compiles AssemblyScript source code into a WASM binary using the `asc` toolchain, and publishes `CompilationResult` messages back to the platform.

## 2. Message Contracts
ProjectNIL already defines the queue DTOs under `services/api/src/main/java/com/projectnil/api/queue/`:

- `CompilationJob`: `{ functionId, language, source }`
- `CompilationResult`: `{ functionId, success, wasmBinary, error }`

The AssemblyScript service must:
1. Subscribe to the **`compilation_jobs`** queue (backed by pgmq) and ignore jobs whose `language` is not `"assemblyscript"`.
2. Publish to **`compilation_results`** using pgmq, base64-encoding the compiled WASM so that the API can persist it as `byte[]`.

Message payloads must remain JSON-compatible with the DTOs so the API can deserialize them directly.

## 3. Runtime Flow
1. **Startup**
   - Load configuration from environment variables (`PG_URI`, queue names, compilation timeouts, temp directory).
   - Establish a pooled pgmq client (e.g., via `pg` or `@neondatabase/serverless`) and verify the target queues exist.

2. **Job Consumption**
   - Poll messages from `compilation_jobs` using pgmq’s `read` semantics with a visibility timeout.
   - Parse JSON into an internal representation mirroring `CompilationJob`.
   - When encountering another language, `archive` the message immediately or toggle visibility so another specialized service can pick it up.

3. **Compilation**
   - Persist the incoming `source` to a temporary `.ts` file (one folder per job). Alternatively, pipe the source directly to `asc` via stdin.
   - Execute `asc <file> --binaryFile <output>.wasm --optimize --measure` using AssemblyScript CLI (installed via npm) or the programmatic API.
   - Capture stdout/stderr for logging and to attach compiler errors back to the API.

4. **Result Publishing**
   - On success: read the `.wasm` artifact, base64-encode it, and publish a `CompilationResult` message with `success: true` and `error: null`.
   - On failure: publish a result with `success: false`, `wasmBinary: null`, and `error` populated with the compiler output.
   - Always delete/archive the original queue message after publishing the result to avoid redelivery.

5. **Cleanup**
   - Remove temporary files/directories for the job to avoid disk growth.
   - Close the pgmq connection gracefully on shutdown.

## 4. Project Structure
```
services/compiler/
├── build.gradle.kts
├── src/main/java/
│   └── com/projectnil/compiler/
│       ├── Application.java     # bootstraps config + messaging
│       ├── CompilerService.java # wraps AssemblyScript compilation via shell
│       └── PgmqClient.java      # queue helpers
├── src/main/resources/
│   └── application.yaml
└── README.md            # service-specific instructions
```

### Entry Points
- `Application.java`: loads env vars/config, wires messaging client, manages retries.
- `PgmqClient.java`: encapsulates queue connectivity, polling, and publishing.
- `CompilerService.java`: exposes `compile(source)` returning `{ success, wasmBinary?, error? }` and shells out to `asc`.

## 5. Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | Connection string for Postgres/pgmq | `postgres://projectnil:projectnil@postgres:5432/projectnil` |
| `JOB_QUEUE` | Queue to consume (default `compilation_jobs`) | optional |
| `RESULT_QUEUE` | Queue to publish results (default `compilation_results`) | optional |
| `COMPILER_TMP_DIR` | Where to store temp files | OS tmp |
| `COMPILER_TIMEOUT_MS` | Max compilation time before failing | `10000` |

Use `dotenv` only for local development; production will inject environment variables via compose.

## 6. Error Handling & Observability
- Log job receipt, compilation duration, and publish status using a structured logger (e.g., `pino`).
- Map fatal issues (e.g., Postgres connection failures) to process exit so orchestrators can restart the container.
- Include truncated compiler stderr in the `error` field to aid debugging.
- Use pgmq’s `read`/`delete`/`archive` semantics to control visibility and avoid message loss. Requeue (via visibility timeout) on transient infrastructure errors; drop on irrecoverable syntax errors after publishing failure results.

## 7. Docker & Compose
- Base image: `eclipse-temurin:25-jdk-alpine` (or equivalent JDK 25 image).
- Use Gradle to build the compiler module inside the container.
- Copy only necessary jars and resources into the final runtime stage.
- Default command: `java -jar compiler.jar`.
- Update `infra/compose.yml` to add the new service, wiring it to the existing pgmq/Postgres container network.

## 8. Testing Strategy for the Compiler Service
- **Unit**: Mock `ProcessBuilder` interactions to verify compiler wrapper logic (temp file handling, error propagation).
- **Integration**: Use Testcontainers with Postgres/pgmq to assert end-to-end flow (job → compilation → result) via JUnit.
- **Smoke**: Compose profile that brings up Postgres + API + compiler JVM service to confirm the full pipeline.

## 9. Adding Additional Compilers
1. Copy the scaffold into a new folder (e.g., `services/compiler-rust`).
2. Implement a language-specific `compile()` adapter while reusing the shared messaging conventions.
3. Register the new service in compose and set `language` filters accordingly.

## 10. Modularity & Shared Interfaces
To keep compiler services consistent across languages, split responsibilities and codify reusable contracts:

### Layered Components
1. **Messaging Adapter** (`messaging.js`): Owns pgmq connection lifecycle, `read/delete/archive` helpers, and JSON serialization. This module exposes a language-agnostic API so other compilers can reuse it by importing from a shared package (future `services/compiler-kit`).
2. **Job Router** (`index.js`): Glues messaging to compilation. It applies language filters, controls concurrency, and handles retries. Business logic remains minimal so it can be copied between language implementations.
3. **Compilation Engine** (`compiler.js`): Implements `compile(job: CompilationJob): Promise<CompilationResultPayload>`. Only this layer changes per language.
4. **Filesystem Workspace** (`workspace.js`, optional): Standardizes how temp directories/files are created, ensuring deterministic cleanup regardless of language.

### Suggested Interfaces (TypeScript-ish pseudocode)
```ts
export interface CompilationJobPayload {
  functionId: string;
  language: string;
  source: string;
}

export interface CompilationOutcome {
  success: boolean;
  wasmBinary?: Buffer;
  error?: string;
  logs?: string;
}

export interface LanguageCompiler {
  language(): string;           // e.g., "assemblyscript"
  compile(job: CompilationJobPayload): Promise<CompilationOutcome>;
}
```

All language services implement `LanguageCompiler` and register it with a common runner (exposed as a shared Java library in the future):
```java
public static void main(String[] args) {
    var runner = CompilerRunner.create(new AssemblyScriptCompiler());
    runner.start();
}
```
The runner handles pgmq polling, base64 encoding, and result publishing, while the compiler focuses on turning `source` into a WASM binary.

### Shared Utilities Roadmap
- **`@projectnil/compiler-kit`** (future package) to house:
  - pgmq client + reconnection logic
  - JSON schema validation for incoming jobs
  - Standard error/result mappers
  - Metrics/logging helpers
- Each new compiler imports the kit and implements only the `compile()` method.

This separation keeps the AssemblyScript implementation clean today and sets us up to onboard Rust/Go compilers quickly.

This document serves as the blueprint for implementing issue #36 using the JVM-based compiler service.
