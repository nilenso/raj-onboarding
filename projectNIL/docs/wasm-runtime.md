# WASM Runtime Implementation Roadmap

**Status**: Draft  
**Related Issues**: #37, #29  
**Canonical Spec**: `scope/contracts.md` (WASM ABI), `scope/practices.md` (security/testing)

This document details the implementation plan for integrating the Chicory WASM runtime into the API service, enabling function execution (US-3).

---

## 1. Overview

### Purpose

The WASM runtime allows the API service to execute compiled WebAssembly modules with JSON input and return JSON output. This is the core execution engine for ProjectNIL's FaaS platform.

### Canonical Contract (from `scope/contracts.md`)

```
- The compiled module MUST export a function named `handle`.
- Signature MUST be logically: `handle(inputJson: string) -> string`.
- `inputJson` and output strings are UTF-8.
```

### Runtime Interface (existing)

```java
// com.projectnil.api.runtime.WasmRuntime
public interface WasmRuntime {
    byte[] execute(byte[] wasmBinary, String inputJson) throws Exception;
}
```

---

## 2. Technical Context

### 2.1 Chicory WASM Runtime

Chicory is a pure-Java WebAssembly runtime (no JNI). Version 1.6.1 is already configured in `gradle/libs.versions.toml`.

**Key Chicory APIs**:

| Class/Method | Purpose |
|--------------|---------|
| `Parser.parse(byte[])` | Parse WASM binary into a `Module` |
| `Instance.builder(module).build()` | Instantiate a module |
| `instance.export("name")` | Get exported function handle |
| `instance.memory()` | Access linear memory |
| `memory.readString(ptr, len)` | Read string from memory |
| `memory.writeString(ptr, value)` | Write string to memory |
| `exportFunction.apply(args...)` | Invoke function with i64 args |

**Resource Limits** (from Chicory docs):
- Thread interruption is honored for timeout enforcement
- Use `ExecutorService` with `Future.get(timeout)` for execution limits

### 2.2 AssemblyScript Memory Model

AssemblyScript compiles `handle(string): string` to WASM as `handle(i32) -> i32` where arguments are **pointers to memory**.

**String Layout in Memory**:
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

**Required Exports** (when compiled with `--exportRuntime`):

| Export | Signature | Purpose |
|--------|-----------|---------|
| `__new` | `(size: i32, id: i32) -> i32` | Allocate managed object |
| `__pin` | `(ptr: i32) -> i32` | Pin object (prevent GC) |
| `__unpin` | `(ptr: i32) -> void` | Unpin object (allow GC) |
| `handle` | `(ptr: i32) -> i32` | User function |
| `memory` | (export) | Linear memory |

### 2.3 Multi-Language Consideration

Different source languages have different memory models:

| Language | String Encoding | Memory Exports |
|----------|-----------------|----------------|
| AssemblyScript | UTF-16LE | `__new`, `__pin`, `__unpin` |
| Rust | UTF-8 | `alloc`, `dealloc` |
| C/C++ | UTF-8 | `malloc`, `free` |
| Go (TinyGo) | UTF-8 | `malloc`, `free` |

**Design Decision**: Abstract string I/O via a `WasmStringCodec` interface. Phase 0 implements AssemblyScript; future phases add language-specific codecs.

---

## 3. Architecture

### 3.1 Component Diagram

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
│                          │  - timeout (configurable)        │  │
│                          │  - stringCodec                   │  │
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

### 3.2 Package Structure

```
services/api/src/main/java/com/projectnil/api/runtime/
├── WasmRuntime.java                    # Interface (exists)
├── ChicoryWasmRuntime.java             # Main implementation
├── WasmStringCodec.java                # Interface for string I/O
├── AssemblyScriptStringCodec.java      # AS-specific implementation
├── WasmExecutionException.java         # Runtime errors (traps, timeouts)
├── WasmAbiException.java               # ABI violations
├── WasmRuntimeProperties.java          # Configuration (@ConfigurationProperties)
└── WasmRuntimeConfiguration.java       # Spring bean wiring
```

---

## 4. Detailed Design

### 4.1 `WasmStringCodec` Interface

Abstracts language-specific string memory handling:

```java
public interface WasmStringCodec {
    
    /**
     * Validates that the module exports all required functions for this codec.
     * @throws WasmAbiException if required exports are missing
     */
    void validateExports(Instance instance) throws WasmAbiException;
    
    /**
     * Writes a Java string into WASM linear memory.
     * @return pointer to the string in WASM memory
     */
    int writeString(Instance instance, String value);
    
    /**
     * Reads a string from WASM linear memory at the given pointer.
     */
    String readString(Instance instance, int pointer);
    
    /**
     * Cleanup any pinned/allocated memory after execution.
     */
    void cleanup(Instance instance, int pointer);
}
```

### 4.2 `AssemblyScriptStringCodec` Implementation

Handles AssemblyScript's UTF-16LE encoding and GC-managed memory:

```java
public class AssemblyScriptStringCodec implements WasmStringCodec {
    
    private static final int STRING_CLASS_ID = 2;
    private static final int RT_SIZE_OFFSET = -4;
    
    @Override
    public void validateExports(Instance instance) throws WasmAbiException {
        // Verify __new, __pin, __unpin, memory exist
    }
    
    @Override
    public int writeString(Instance instance, String value) {
        // 1. Convert Java String to UTF-16LE bytes
        // 2. Call __new(byteLength, STRING_CLASS_ID)
        // 3. Call __pin(ptr)
        // 4. Write UTF-16LE bytes to memory at ptr
        // 5. Return ptr
    }
    
    @Override
    public String readString(Instance instance, int pointer) {
        // 1. Read rtSize from (pointer - 4)
        // 2. Read rtSize bytes from pointer
        // 3. Decode UTF-16LE to Java String
    }
    
    @Override
    public void cleanup(Instance instance, int pointer) {
        // Call __unpin(pointer)
    }
}
```

### 4.3 `ChicoryWasmRuntime` Implementation

Main runtime orchestrating parse, instantiate, execute:

```java
public class ChicoryWasmRuntime implements WasmRuntime {
    
    private final WasmStringCodec stringCodec;
    private final Duration timeout;
    
    @Override
    public byte[] execute(byte[] wasmBinary, String inputJson) throws Exception {
        // 1. Parse WASM binary
        Module module = Parser.parse(wasmBinary);
        
        // 2. Instantiate
        Instance instance = Instance.builder(module).build();
        
        // 3. Validate ABI
        validateHandleExport(instance);
        stringCodec.validateExports(instance);
        
        // 4. Write input to memory
        int inputPtr = stringCodec.writeString(instance, inputJson);
        
        try {
            // 5. Execute with timeout
            ExportFunction handle = instance.export("handle");
            int outputPtr = executeWithTimeout(handle, inputPtr);
            
            // 6. Read output
            String output = stringCodec.readString(instance, outputPtr);
            return output.getBytes(StandardCharsets.UTF_8);
            
        } finally {
            // 7. Cleanup
            stringCodec.cleanup(instance, inputPtr);
        }
    }
    
    private int executeWithTimeout(ExportFunction fn, int arg) 
            throws WasmExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<long[]> future = executor.submit(() -> fn.apply(arg));
        try {
            long[] result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return (int) result[0];
        } catch (TimeoutException e) {
            future.cancel(true); // Triggers thread interrupt
            throw new WasmExecutionException(
                "Execution timed out after " + timeout.toSeconds() + "s");
        } catch (ExecutionException e) {
            throw new WasmExecutionException(
                "WASM trap: " + e.getCause().getMessage(), e.getCause());
        } finally {
            executor.shutdownNow();
        }
    }
    
    private void validateHandleExport(Instance instance) throws WasmAbiException {
        try {
            instance.export("handle");
        } catch (Exception e) {
            throw new WasmAbiException(
                "Module must export a 'handle' function");
        }
    }
}
```

### 4.4 Exception Hierarchy

```java
// Runtime errors: traps, timeouts, invalid output
public class WasmExecutionException extends RuntimeException {
    public WasmExecutionException(String message) { super(message); }
    public WasmExecutionException(String message, Throwable cause) { 
        super(message, cause); 
    }
}

// ABI violations: missing exports, wrong signatures
public class WasmAbiException extends RuntimeException {
    public WasmAbiException(String message) { super(message); }
}
```

### 4.5 Configuration

```java
@ConfigurationProperties(prefix = "projectnil.wasm")
public record WasmRuntimeProperties(
    Duration timeout
) {
    public WasmRuntimeProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(10);
        }
    }
}
```

**application.yaml**:
```yaml
projectnil:
  wasm:
    timeout: 10s
```

### 4.6 Spring Configuration

```java
@Configuration
@EnableConfigurationProperties(WasmRuntimeProperties.class)
public class WasmRuntimeConfiguration {
    
    @Bean
    public WasmStringCodec wasmStringCodec() {
        return new AssemblyScriptStringCodec();
    }
    
    @Bean
    public WasmRuntime wasmRuntime(
            WasmStringCodec stringCodec,
            WasmRuntimeProperties properties) {
        return new ChicoryWasmRuntime(stringCodec, properties.timeout());
    }
}
```

---

## 5. Test Strategy

### 5.1 Test Resources

Pre-compiled WASM modules in `services/api/src/test/resources/wasm/`:

| File | AssemblyScript Source | Expected Behavior |
|------|----------------------|-------------------|
| `echo.wasm` | Returns input as-is | `{"x":1}` -> `{"x":1}` |
| `add.wasm` | Parses and adds numbers | `{"a":5,"b":3}` -> `{"sum":8}` |
| `greet.wasm` | String concatenation | `{"name":"Alice"}` -> `{"greeting":"Hello, Alice!"}` |
| `trap.wasm` | Calls `unreachable` | Throws `WasmExecutionException` |
| `no-handle.wasm` | Missing handle export | Throws `WasmAbiException` |
| `infinite-loop.wasm` | Never terminates | Times out, throws `WasmExecutionException` |

### 5.2 Creating Test Modules

AssemblyScript source for test modules:

**echo.ts**:
```typescript
export function handle(input: string): string {
    return input;
}
```

**add.ts**:
```typescript
import { JSON } from "assemblyscript-json";

export function handle(input: string): string {
    const obj = <JSON.Obj>JSON.parse(input);
    const a = obj.getNum("a")!.valueOf();
    const b = obj.getNum("b")!.valueOf();
    const result = new JSON.Obj();
    result.set("sum", a + b);
    return result.stringify();
}
```

**Compilation command**:
```bash
npx asc src/echo.ts \
    --outFile build/echo.wasm \
    --exportRuntime \
    --runtime incremental \
    --optimize
```

### 5.3 Test Classes

**Unit Tests** (`ChicoryWasmRuntimeTest.java`):
- No Spring context
- Load pre-compiled `.wasm` from test resources
- Test success, trap, ABI violation, and timeout scenarios

**Integration Tests** (future, when ExecutionService exists):
- Full Spring context
- Test end-to-end execution flow

### 5.4 Test Cases

| Test | Input | Expected |
|------|-------|----------|
| `execute_echo_returnsInputUnchanged` | `{"foo":"bar"}` | `{"foo":"bar"}` as bytes |
| `execute_add_computesSum` | `{"a":10,"b":5}` | `{"sum":15}` as bytes |
| `execute_greet_concatenatesString` | `{"name":"World"}` | `{"greeting":"Hello, World!"}` |
| `execute_trap_throwsExecutionException` | any | `WasmExecutionException` |
| `execute_noHandle_throwsAbiException` | any | `WasmAbiException` |
| `execute_infiniteLoop_timesOut` | any | `WasmExecutionException` with timeout message |

---

## 6. Implementation Checklist

### Phase 1: Core Runtime

- [ ] Create `WasmExecutionException.java`
- [ ] Create `WasmAbiException.java`
- [ ] Create `WasmStringCodec.java` interface
- [ ] Create `AssemblyScriptStringCodec.java` implementation
- [ ] Create `ChicoryWasmRuntime.java` implementation
- [ ] Create `WasmRuntimeProperties.java` configuration
- [ ] Create `WasmRuntimeConfiguration.java` Spring config
- [ ] Add configuration to `application.yaml`

### Phase 2: Test Resources

- [ ] Create `services/api/src/test/resources/wasm/` directory
- [ ] Write AssemblyScript source files for test modules
- [ ] Compile and commit `.wasm` test files
- [ ] Create `ChicoryWasmRuntimeTest.java`

### Phase 3: Validation & Documentation

- [ ] Verify all tests pass
- [ ] Update `scope/contracts.md` if any ABI clarifications needed
- [ ] Update GitHub issue #37 with implementation notes

---

## 7. Design Decisions

| Question | Decision | Rationale |
|----------|----------|-----------|
| Where should test WASM files live? | `src/test/resources/wasm/` - pre-compiled, checked into git | Deterministic tests, no build-time dependencies |
| UTF-8 vs UTF-16? | Handle UTF-16LE internally (AS native), return UTF-8 to caller | Match AssemblyScript's native encoding |
| Timeout mechanism? | `ExecutorService` + thread interrupt (10s default) | Simple, leverages Chicory's interrupt support |
| Multi-language support? | Abstract via `WasmStringCodec`, implement AS-specific for Phase 0 | Extensible without runtime changes |
| Error message sanitization | Include trap type but sanitize stack traces (e.g., `"WASM trap: unreachable instruction"`) | Balance of debugging utility and security |
| Module caching | No caching for Phase 0 - parse fresh each execution | Simplicity first; caching is a future enhancement |
| Memory limits | Log warning but allow execution | Observe usage patterns first, enforce limits later |

---

## 8. Future Enhancements (Post-Phase 0)

### 8.1 Additional Language Codecs

When adding Rust/Go support:

1. Create `RustStringCodec` implementing `WasmStringCodec`
2. Update compiler service to generate compatible wrapper code
3. Select codec based on `Function.language` field

### 8.2 Standard ABI (Long-term)

Define a language-agnostic ABI that all compilers must target:

```
Required exports:
  - handle(inputPtr: i32, inputLen: i32) -> i32
  - getOutputLen() -> i32
  - alloc(size: i32) -> i32
  - free(ptr: i32) -> void
```

This eliminates language-specific codecs by standardizing memory management.

### 8.3 Performance Optimizations

- **Module caching**: Cache parsed modules by function ID
- **Instance pooling**: Reuse instances for repeated executions
- **AOT compilation**: Use Chicory's AOT compiler for hot functions

### 8.4 Enhanced Resource Limits

- Memory limits via WASM memory max pages
- Fuel-based instruction counting (if Chicory adds support)
- Execution metrics logging (duration, memory high-water mark)

---

## 9. References

- [Chicory Documentation](https://chicory.dev/docs/)
- [Chicory GitHub](https://github.com/dylibso/chicory)
- [AssemblyScript Documentation](https://www.assemblyscript.org/)
- [WebAssembly Specification](https://webassembly.github.io/spec/)
- ADR 001: `docs/decisions/001-wasm-runtime.md`
- Canonical Contracts: `scope/contracts.md`
