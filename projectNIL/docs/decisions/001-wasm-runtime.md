# ADR 001: WASM Runtime

**Status**: Accepted (December 2025)

## Context

How to store and execute user-submitted functions safely?

## Decision

**WebAssembly modules** executed via **Chicory** (pure Java runtime).

- Storage: WASM binary in PostgreSQL BYTEA
- Compilation: Server-side via compiler microservices
- First language: AssemblyScript

## Options Considered

| Option | Verdict |
|--------|---------|
| Code string (interpreted) | Security risk |
| Container image | Too complex for Phase 0 |
| **WASM modules** | Best balance |
| External storage (S3) | Unnecessary complexity |
| PostgreSQL stored procs | Security risk |

## Rationale

**Why WASM?**
- Sandboxed by design (memory isolated, explicit imports)
- Near-native performance
- Language-agnostic (Rust, Go, AS all compile to WASM)
- Portable binary format

**Why Chicory?**
- Pure Java (no JNI, simple deployment)
- Easy to embed and debug
- Active development (Dylibso/Shopify)

**Why server-side compilation?**
- Better DX (no local toolchain needed)
- Consistent environment
- Can validate source before compiling

**Why PostgreSQL BYTEA?**
- Single data store
- Transactional consistency
- Sufficient for KB-MB binaries

## Consequences

**Positive**: Secure execution, clean architecture, extensible to multiple languages

**Negative**: Compilation infrastructure needed, WASM data passing complexity, debugging harder than interpreted code

## References

- [Chicory](https://github.com/nicksanford/chicory)
- [AssemblyScript](https://www.assemblyscript.org/)
- [WebAssembly Spec](https://webassembly.github.io/spec/)
