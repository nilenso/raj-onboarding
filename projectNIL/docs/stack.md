# Technology Stack - ProjectNIL

## Overview

ProjectNIL is a **Function as a Service (FaaS)** platform that allows users to submit source code in various languages, compiles it to WebAssembly (WASM), and executes it on demand in a sandboxed environment.

This document captures the technology choices and rationale for the project.

## Architecture Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             User Request                                        │
│         POST /functions { "language": "assemblyscript", "source": "..." }       │
└─────────────────────────────────────┬───────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         API Service (Spring Boot)                               │
│                                                                                 │
│   • REST API (CRUD for functions, executions)                                   │
│   • Persists source with status=PENDING                                         │
│   • Publishes compilation job to pgmq (message queue in PostgreSQL)             │
│   • Consumes compilation results, updates DB with WASM binary                   │
│   • Executes WASM via Chicory runtime                                           │
└───────────┬─────────────────────────────────────────────────────────────────────┘
            │                                              ▲
            │ Publish: compile.{language}                  │ Consume: compilation.results
            ▼                                              │
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          pgmq (PostgreSQL Queue)                                │
│   Queue: compilation_jobs (routing by language in message)                      │
│   Queue: compilation_results                                                    │
└───────────┬─────────────────────────────────────────────────────────────────────┘
            │                                              ▲
            │ Consume: compile.assemblyscript              │ Publish
            ▼                                              │
┌─────────────────────────────────────────────────────────────────────────────────┐
│                  Compiler Service (Node.js) - implements Compiler Interface     │
│                                                                                 │
│   • Consumes compilation requests                                               │
│   • Compiles source → WASM using asc                                            │
│   • Publishes result (binary or error)                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             PostgreSQL                                          │
│   • functions: id, name, language, source, wasm_binary, status, ...             │
│   • executions: id, function_id, input, output, status, ...                     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Core Technologies

### API Service

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 25 | Primary language |
| **Spring Boot** | 3.4.x | Application framework |
| **Spring Web** | - | REST API endpoints |
| **Spring Data JPA** | - | Database access |
| **pgmq** | - | PostgreSQL-based message queue |
| **Gradle** | 8.x | Build tool |

**Why Spring Boot?**
- Industry standard for Java microservices
- Excellent ecosystem (data, messaging, security)
- Auto-configuration reduces boilerplate
- Strong community and documentation
- Good fit for learning the Java enterprise ecosystem

**Why Java 25?**
- Latest release with modern language features
- Required minimum for Spring Boot 3.x is Java 17
- Java 25 brings latest virtual threads, pattern matching, and other modern features
- Strong performance improvements and language enhancements

**Why Gradle over Maven?**
- More flexible build scripts (Groovy/Kotlin DSL)
- Better incremental build performance
- Version catalogs for dependency management
- Already initialized in this project

### Database

| Technology | Version | Purpose |
|------------|---------|---------|
| **PostgreSQL** | 16.x | Primary database |
| **Liquibase** | 4.x | Schema migrations |

**Why PostgreSQL?**
- Robust, open-source relational database
- Excellent JSON/JSONB support for flexible data
- BYTEA type for storing WASM binaries
- Native ENUM support for status fields
- Strong ecosystem and tooling

**Why Liquibase over Flyway?**
- More flexible changelog formats (YAML, XML, SQL)
- Better rollback support
- Changelog includes support for preconditions
- Good Spring Boot integration
- Both are excellent; Liquibase chosen for learning breadth

### Message Queue

| Technology | Version | Purpose |
|------------|---------|---------|
| **pgmq** | Latest | PostgreSQL-native message queue |

**Why pgmq?**
- Built on PostgreSQL (single data store)
- No additional infrastructure (AMQP broker, containers, ports)
- Native SQL-based queuing with FIFO guarantees
- Simple API with automatic message cleanup
- Reduces operational complexity for Phase 0
- Scales with PostgreSQL (already in use)

**Why async compilation via message queue?**
- Decouples API service from compiler services
- Allows independent scaling of compilers
- Enables adding new language compilers without API changes
- Handles slow compilations (e.g., Rust) without blocking HTTP requests
- Natural buffering during traffic spikes

### WASM Runtime

| Technology | Version | Purpose |
|------------|---------|---------|
| **Chicory** | 1.x | WASM execution in JVM |

**Why WebAssembly?**
- Sandboxed execution (security)
- Language-agnostic (compile from multiple source languages)
- Near-native performance
- Portable binary format
- Growing ecosystem and industry adoption

**Why Chicory?**
- Pure Java implementation (no JNI, no native dependencies)
- Easy to embed and debug
- Active development (Dylibso/Shopify)
- Simpler deployment vs native runtimes (Wasmtime, Wasmer)

See [ADR 001: WASM Runtime](./decisions/001-wasm-runtime.md) for detailed decision rationale.

### Compiler Services

| Technology | Purpose |
|------------|---------|
| **Node.js 20.x** | AssemblyScript compiler runtime |
| **AssemblyScript (asc)** | TypeScript-like → WASM compiler |

**Compiler Interface Design**

Each compiler service, regardless of implementation language, must:

1. **Consume** from queue: `compile.{language}`
2. **Publish** to queue: `compilation.results`
3. **Message format**: Defined contract (see Phase 0 docs)
4. **Behavior**: Stateless, idempotent

This allows adding new language compilers without modifying the API service:

| Language | Compiler | Status |
|----------|----------|--------|
| AssemblyScript | `asc` (Node.js) | Phase 0 |
| Rust | `rustc` + `wasm-pack` | Future |
| Go | TinyGo | Future |
| C/C++ | Emscripten | Future |

**Why AssemblyScript first?**
- TypeScript-like syntax (familiar to most developers)
- Fast compilation (milliseconds)
- Designed specifically for WASM
- Lightweight toolchain (just Node.js)

### Containerization

| Technology | Purpose |
|------------|---------|
| **Podman Compose** | Local development environment |

**Why Podman over Docker?**
- Daemonless (no root daemon process)
- Compatible with Docker Compose files
- Better security model
- Drop-in replacement for most Docker workflows

## Service Breakdown

| Service | Technology | Port | Purpose |
|---------|------------|------|---------|
| api-service | Spring Boot | 8080 | REST API, DB access, WASM execution |
| compiler-assemblyscript | Node.js | N/A (queue only) | Compile AS → WASM |
| postgres | PostgreSQL | 5432 | Database + pgmq message queue |

## Architecture Decisions

### Why No Service Discovery (Eureka)?

We considered Spring Cloud Netflix Eureka for service discovery but decided against it for Phase 0:

- **pgmq provides decoupling**: Services communicate via database queues, not direct HTTP calls
- **No dynamic scaling yet**: Fixed set of services for Phase 0
- **Reduced complexity**: One less moving part to manage
- **Can add later**: Architecture supports adding Eureka if needed

### Why Server-Side Compilation?

Users submit source code, not pre-compiled WASM binaries:

**Pros:**
- Better developer experience (no local toolchain needed)
- Consistent compilation environment
- Can validate/sanitize source before compilation
- Easier to add new languages

**Cons:**
- Server bears compilation cost
- Need to manage compiler toolchains
- Potential security surface (compiling arbitrary code)

Decision: Accept the complexity for better UX and learning opportunity.

### Why Store WASM in PostgreSQL (BYTEA)?

Alternatives considered:
- S3/object storage
- Filesystem
- Separate WASM registry

Decision: PostgreSQL BYTEA for Phase 0:
- Simplicity (single data store)
- Transactional consistency
- Sufficient for expected binary sizes (KB-MB range)
- Can migrate to object storage if needed

## Project Structure

```
projectNIL/
├── services/
│   ├── api-service/                        # Spring Boot
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/projectnil/api/
│   │       │   │   ├── ApiServiceApplication.java
│   │       │   │   ├── controller/
│   │       │   │   ├── service/
│   │       │   │   ├── repository/
│   │       │   │   ├── model/
│   │       │   │   ├── messaging/
│   │       │   │   └── runtime/
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── db/changelog/
│   │       └── test/
│   │
│   └── compiler-assemblyscript/            # Node.js
│       ├── package.json
│       ├── src/
│       │   ├── index.js
│       │   ├── compiler.js
│       │   └── messaging.js
│       └── Dockerfile
│
├── gradle/
│   └── libs.versions.toml
├── settings.gradle
├── build.gradle
├── compose.yml
└── docs/
```

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Chicory WASM Runtime](https://github.com/nicksanford/chicory)
- [AssemblyScript](https://www.assemblyscript.org/)
- [pgmq Documentation](https://github.com/tembo-io/pgmq)
- [Liquibase Documentation](https://docs.liquibase.com/)
