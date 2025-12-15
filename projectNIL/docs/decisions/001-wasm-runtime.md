# ADR 001: Function Body Storage - WASM Runtime

## Status

**Accepted** (December 2025)

## Context

ProjectNIL is a Function as a Service (FaaS) platform. A core design decision is how to store and execute user-submitted functions.

The function "body" needs to be:
- Stored persistently
- Executed safely (sandboxed)
- Performant
- Language-agnostic (ideally)

## Options Considered

### Option 1: Code String (Interpreted at Runtime)

Store source code as text, interpret/evaluate at runtime.

| Aspect | Assessment |
|--------|------------|
| Storage | Simple (TEXT column) |
| Security | High risk (code injection, sandbox escapes) |
| Performance | Slow (interpretation overhead) |
| Languages | Limited to interpreted languages (JS, Python) |
| Complexity | Low |

**Verdict**: Too risky for arbitrary user code.

### Option 2: Container Image Reference

Store reference to Docker/OCI image containing the function.

| Aspect | Assessment |
|--------|------------|
| Storage | Simple (URL/reference string) |
| Security | Good (container isolation) |
| Performance | Slow cold starts, resource heavy |
| Languages | Any (full runtime in container) |
| Complexity | High (container orchestration) |

**Verdict**: Overkill for Phase 0, too much infrastructure.

### Option 3: WebAssembly (WASM) Modules

Compile source to WASM, store binary, execute in WASM runtime.

| Aspect | Assessment |
|--------|------------|
| Storage | Binary (BYTEA column, ~KB-MB) |
| Security | Excellent (sandboxed by design) |
| Performance | Near-native |
| Languages | Many (Rust, C, Go, AssemblyScript, etc.) |
| Complexity | Medium (need compiler services) |

**Verdict**: Best balance of security, performance, and flexibility.

### Option 4: External Reference (S3/Storage URL)

Store function artifact in object storage, reference by URL.

| Aspect | Assessment |
|--------|------------|
| Storage | URL string + external binary |
| Security | Depends on execution method |
| Performance | Extra network hop |
| Languages | Depends on artifact format |
| Complexity | Medium (external dependency) |

**Verdict**: Adds unnecessary complexity for Phase 0.

### Option 5: PostgreSQL Stored Procedures

Store function as PL/pgSQL or PL/Python procedure in PostgreSQL.

| Aspect | Assessment |
|--------|------------|
| Storage | In database (native) |
| Security | Risky (runs inside database!) |
| Performance | Fast (no network hop for data) |
| Languages | Limited (PL/pgSQL, PL/Python, PL/v8) |
| Complexity | Low |

**Verdict**: Security risk too high (user code in database).

## Decision

**WebAssembly (WASM) modules** with the following specifics:

1. **Storage**: WASM binary in PostgreSQL BYTEA column
2. **Runtime**: Chicory (pure Java WASM interpreter)
3. **Compilation**: Server-side via dedicated compiler microservices
4. **Source languages**: AssemblyScript for Phase 0, extensible to Rust, Go, etc.

## Rationale

### Why WASM?

1. **Security**: WASM is sandboxed by design. Memory is isolated, system calls are explicitly imported. A malicious function cannot access the host filesystem, network, or other processes.

2. **Performance**: WASM executes at near-native speed. Chicory interprets WASM bytecode efficiently, and future JIT compilation can improve this further.

3. **Language Agnostic**: Many languages compile to WASM (Rust, C/C++, Go, AssemblyScript, Kotlin, Swift, Zig). This allows future expansion without changing the execution model.

4. **Portable**: WASM binaries are platform-independent. The same binary runs on any WASM runtime.

5. **Growing Ecosystem**: WASM is increasingly adopted for serverless, edge computing, and plugin systems. Skills and tooling are improving rapidly.

### Why Chicory?

1. **Pure Java**: No JNI bindings, no native library dependencies. Simplifies deployment and debugging.

2. **Embeddable**: Designed to be embedded in Java applications. Clean API for loading and executing modules.

3. **Active Development**: Maintained by Dylibso with contributions from Shopify. Regular releases and improvements.

4. **Sufficient for Phase 0**: May not be the fastest runtime, but performance is acceptable for learning project scope.

### Why Server-Side Compilation?

Users submit source code (e.g., AssemblyScript/TypeScript), not pre-compiled WASM:

1. **Better DX**: Users don't need local compiler toolchains
2. **Consistency**: Compilation environment is controlled
3. **Validation**: Can inspect/validate source before compilation
4. **Extensibility**: Add new languages by adding compiler services

### Why PostgreSQL BYTEA?

1. **Simplicity**: Single data store for metadata and binary
2. **Transactions**: WASM binary stored atomically with function metadata
3. **Sufficient**: Expected binary sizes (KB to low MB) fit comfortably
4. **Migratable**: Can move to object storage later if needed

## Consequences

### Positive

- Secure execution of arbitrary user code
- Clean architecture (compilation separate from execution)
- Extensible to multiple source languages
- Modern, industry-aligned technology choice
- Strong learning opportunity (WASM is valuable skill)

### Negative

- Compilation infrastructure needed (compiler microservices)
- Data passing complexity (WASM only understands primitives, need serialization for JSON)
- Debugging WASM is harder than debugging interpreted code
- Chicory is newer/less battle-tested than Wasmtime/Wasmer

### Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| WASM data passing is complex | Start with simple int→int functions, add JSON serialization incrementally |
| Chicory performance issues | Acceptable for learning project; can switch to Wasmtime if needed |
| Compiler service failures | Message queue provides buffering and retry capability |
| Large WASM binaries | Enforce size limits; monitor and optimize |

## References

- [WebAssembly Specification](https://webassembly.github.io/spec/)
- [Chicory GitHub](https://github.com/nicksanford/chicory)
- [AssemblyScript](https://www.assemblyscript.org/)
- [WASM in the Cloud](https://www.fermyon.com/blog/webassembly-vs-containers)
- [Bytecode Alliance](https://bytecodealliance.org/)

## Related

- GitHub Issue: #23 (Spike: Research function body storage strategies)
- Documentation: `docs/stack.md`
- Phase 0 Scope: `docs/phase0.md`
