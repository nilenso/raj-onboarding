# WASM Runtime Implementation

**Status**: Implemented (PR #37)  
**Related Issues**: #37, #29  
**Canonical Spec**: `scope/contracts.md` (WASM ABI), `scope/practices.md` (security/testing)

This document describes the WASM runtime implementation for executing compiled AssemblyScript functions.

---

## 1. Overview

### Purpose

The WASM runtime allows the API service to execute compiled WebAssembly modules with JSON input and return JSON output. This is the core execution engine for ProjectNIL's FaaS platform.

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

### Canonical Contract (from `scope/contracts.md`)

- The compiled module MUST export a function named `handle`
- Signature MUST be logically: `handle(inputJson: string) -> string`
- Input/output strings are UTF-8 JSON at the API boundary

### Runtime Interface

```java
// com.projectnil.api.runtime.WasmRuntime
public interface WasmRuntime {
    byte[] execute(byte[] wasmBinary, String inputJson) throws Exception;
}
```

---

## 2. Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Service                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐     ┌──────────────────────────────────┐  │
│  │ ExecutionService│────>│ WasmRuntime (interface)          │  │
│  └─────────────────┘     └──────────────────────────────────┘  │
│                                        │                        │
│                                        ▼                        │
│                          ┌──────────────────────────────────┐  │
│                          │ ChicoryWasmRuntime               │  │
│                          │  - timeout (10s default)         │  │
│                          │  - stringCodec                   │  │
│                          │  - env.abort host function       │  │
│                          └──────────────────────────────────┘  │
│                                        │                        │
│                          ┌─────────────┴─────────────┐         │
│                          ▼                           ▼         │
│              ┌────────────────────┐    ┌────────────────────┐  │
│              │ WasmStringCodec    │    │ Chicory Library    │  │
│              │ (interface)        │    │ (Parser, Instance) │  │
│              └────────────────────┘    └────────────────────┘  │
│                          │                                      │
│                          ▼                                      │
│              ┌────────────────────────────────────────────┐    │
│              │ AssemblyScriptStringCodec                  │    │
│              │  - writeString (UTF-16LE + __new/__pin)    │    │
│              │  - readString (UTF-16LE from memory)       │    │
│              │  - cleanup (__unpin)                       │    │
│              └────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
services/api/src/main/java/com/projectnil/api/runtime/
├── WasmRuntime.java                    # Interface
├── ChicoryWasmRuntime.java             # Main implementation
├── WasmStringCodec.java                # Interface for string I/O
├── AssemblyScriptStringCodec.java      # AS-specific implementation
├── WasmExecutionException.java         # Runtime errors (traps, timeouts)
├── WasmAbiException.java               # ABI violations
├── WasmRuntimeProperties.java          # Configuration
└── WasmRuntimeConfiguration.java       # Spring bean wiring
```

---

## 3. Key Abstractions

### 3.1 WasmRuntime Interface

The contract for executing WASM modules:

```java
public interface WasmRuntime {
    byte[] execute(byte[] wasmBinary, String inputJson) throws Exception;
}
```

Takes compiled WASM binary + JSON input → returns JSON output as UTF-8 bytes.

### 3.2 WasmStringCodec Interface

Abstracts **how strings are passed** between Java and WASM memory. Different languages have different memory models:

| Language | Encoding | Memory Management |
|----------|----------|-------------------|
| AssemblyScript | UTF-16LE | GC with `__new`/`__pin`/`__unpin` |
| Rust (future) | UTF-8 | Manual `alloc`/`dealloc` |
| Go (future) | UTF-8 | `malloc`/`free` |

This abstraction allows adding new languages without changing the runtime.

```java
public interface WasmStringCodec {
    void validateExports(Instance instance) throws WasmAbiException;
    int writeString(Instance instance, String value);
    String readString(Instance instance, int pointer);
    void cleanup(Instance instance, int pointer);
}
```

### 3.3 Host Functions

WASM modules can import functions from the host. AssemblyScript requires `env.abort` for error handling. The runtime provides this as a host function that throws `WasmExecutionException` when called.

```java
// Provided by ChicoryWasmRuntime
HostFunction abortFn = new HostFunction(
    "env", "abort",
    FunctionType.of(List.of(I32, I32, I32, I32), List.of()),
    (Instance inst, long... args) -> {
        throw new WasmExecutionException("AssemblyScript abort: ...");
    }
);
```

---

## 4. AssemblyScript Memory Model

AssemblyScript compiles `handle(string): string` to WASM as `handle(i32) -> i32` where arguments are **pointers to memory**.

### String Layout in Memory

```
┌─────────────────────────────────────────────────────────────┐
│  20-byte header (GC metadata)  │  UTF-16LE payload          │
├────────────────────────────────┼─────────────────────────────┤
│  ... | rtId (4B) | rtSize (4B) │  string data (rtSize bytes) │
└────────────────────────────────┴─────────────────────────────┘
         offset -8    offset -4    offset 0 (pointer location)
```

- **rtId**: Class ID (always `2` for strings)
- **rtSize**: Byte length of payload
- **Encoding**: UTF-16LE (2 bytes per character)

### Required Module Exports

When compiled with `--exportRuntime`, AssemblyScript modules export:

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

## 5. Error Handling

### Exception Hierarchy

```java
// Runtime errors: traps, timeouts, invalid output
public class WasmExecutionException extends RuntimeException

// ABI violations: missing exports, wrong signatures  
public class WasmAbiException extends RuntimeException
```

### Error Sanitization

Errors are sanitized to include the trap type without exposing internal stack traces:

| Raw Error | Sanitized Message |
|-----------|-------------------|
| `com.dylibso...TrapException: unreachable` | `WASM trap: unreachable instruction` |
| `TimeoutException` | `Execution timed out after 10 seconds` |
| Module missing `handle` | `Module must export a 'handle' function` |

---

## 6. Configuration

### application.yaml

```yaml
projectnil:
  wasm:
    timeout: 10s
```

### WasmRuntimeProperties

```java
@ConfigurationProperties(prefix = "projectnil.wasm")
public record WasmRuntimeProperties(Duration timeout) {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    
    public WasmRuntimeProperties {
        if (timeout == null) timeout = DEFAULT_TIMEOUT;
    }
}
```

### Resource Limits

| Limit | Implementation | Default |
|-------|----------------|---------|
| Execution timeout | `ExecutorService` + thread interrupt | 10 seconds |
| Memory warning | Log when > 256 pages (16MB) | Warning only |
| String size sanity check | Rejects strings > 10MB | 10MB |

---

## 6.1 Implementation Details (Technical Reference)

This section documents implementation details that may be useful for debugging or future enhancements.

### Thread Model

- **One executor per execution**: A new `Executors.newSingleThreadExecutor()` is created for each `execute()` call
- **Thread safety**: The runtime is thread-safe - each execution is completely independent
- **Interrupt handling**: Timeout uses `future.cancel(true)` which relies on Chicory honoring thread interrupts
- **Resource cleanup**: Executor is always shut down in `finally` block with `shutdownNow()`

### Memory Management Details

- **No hard memory limit**: Memory can grow without bound; only warning is logged
- **Memory growth**: No explicit limit on WASM memory growth during execution
- **Input pointer pinning**: Only the input pointer is pinned/unpinned
- **Output pointer**: Not pinned - relies on AS GC not running during the synchronous read

### String Handling

- **String class ID**: Hardcoded as `2` for AssemblyScript strings
- **RT_SIZE_OFFSET**: `-4` bytes (rtSize stored 4 bytes before pointer)
- **10MB sanity check**: Rejects string sizes > 10MB to prevent memory corruption attacks
- **Log truncation**: Input/output values truncated to 100 characters in logs

### Null Return Handling

When the WASM `handle` function returns a null pointer (0):
- Returns `null` from `readString()`
- `execute()` throws `WasmExecutionException("WASM function returned null")`

### Abort Message Extraction

When AssemblyScript calls `abort()`:
- The runtime attempts to read the abort message from memory using the codec
- If reading fails (defensive), only the raw error is logged
- The exception message includes the abort location (file, line, column)

### Cleanup Behavior

- **Best-effort unpinning**: `__unpin` is called in a finally block
- **Cleanup failures**: Logged as warnings but do not cause execution to fail
- **Already cleaned up**: No error if cleanup is called multiple times

---

## 7. Testing

### Test Resources

Pre-compiled WASM modules in `services/api/src/test/resources/wasm/`:

| File | Purpose | Expected Behavior |
|------|---------|-------------------|
| `echo.wasm` | Returns input as-is | `{"x":1}` → `{"x":1}` |
| `add.wasm` | Adds two numbers | `{"a":5,"b":3}` → `{"sum":8}` |
| `greet.wasm` | String concatenation | `{"name":"Alice"}` → `{"greeting":"Hello, Alice!"}` |
| `trap.wasm` | Triggers unreachable | `WasmExecutionException` |
| `no-handle.wasm` | Missing handle export | `WasmAbiException` |
| `infinite-loop.wasm` | Never terminates | Timeout `WasmExecutionException` |

### Test Coverage

13 unit tests covering:
- **Success scenarios** (7): echo, add, greet, negative numbers, empty JSON, unicode
- **ABI validation** (3): missing handle, invalid binary, empty binary
- **Runtime errors** (2): trap, timeout
- **Configuration** (1): custom timeout

### Test Coverage Gaps (Known)

The following edge cases are not covered by unit tests:
- Memory warning threshold being triggered (>16MB)
- Abort host function behavior (AS abort() call)
- Cleanup failure path (when `__unpin` fails)
- 10MB string size limit rejection
- Null pointer returned from handle
- Integration test with `WasmRuntimeConfiguration` Spring beans

### Compiling Test Modules

```bash
npm install assemblyscript
npx asc echo.ts --outFile echo.wasm --exportRuntime --runtime incremental --optimize
```

---

## 8. Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Test WASM location | `src/test/resources/wasm/` (pre-compiled) | Deterministic tests, no build-time Node.js dependency |
| String encoding | Handle UTF-16LE internally | Match AssemblyScript's native encoding |
| Timeout mechanism | `ExecutorService` + thread interrupt | Simple, leverages Chicory's interrupt support |
| Multi-language support | `WasmStringCodec` abstraction | Extensible without runtime changes |
| Error sanitization | Include trap type, hide stack traces | Balance debugging utility and security |
| Module caching | None for Phase 0 | Simplicity first; caching is future enhancement |
| Memory limits | Log warning only | Observe usage patterns first |

---

## 9. Future Enhancements

### Additional Language Support

When adding Rust/Go support:
1. Create `RustStringCodec` implementing `WasmStringCodec`
2. Update compiler service to generate compatible modules
3. Select codec based on `Function.language` field

### Standard ABI (Long-term)

Define a language-agnostic ABI:
```
Required exports:
  - handle(inputPtr: i32, inputLen: i32) -> i32
  - getOutputLen() -> i32
  - alloc(size: i32) -> i32
  - free(ptr: i32) -> void
```

### Performance Optimizations

- **Module caching**: Cache parsed modules by function ID
- **Instance pooling**: Reuse instances for repeated executions
- **AOT compilation**: Use Chicory's AOT compiler for hot functions

---

## 10. References

- [Chicory Documentation](https://chicory.dev/docs/)
- [Chicory GitHub](https://github.com/dylibso/chicory)
- [AssemblyScript Runtime](https://www.assemblyscript.org/runtime.html)
- [WebAssembly Specification](https://webassembly.github.io/spec/)
- ADR 001: `docs/decisions/001-wasm-runtime.md`
