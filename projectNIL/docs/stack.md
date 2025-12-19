# Technology Stack

This document maintains the current versions and rationale for the ProjectNIL technology stack.

## Core Runtime

| Component | Technology | Version | Rationale |
|-----------|------------|---------|-----------|
| **Java** | OpenJDK | 25 | Latest features, structured concurrency, and virtual thread improvements. |
| **Framework** | Spring Boot | 4.0.0 | Built on Spring Framework 7, providing modularization and first-class Java 25 support. |
| **Build Tool** | Gradle | 8.x | Flexible multi-project build support. |

## Data & Messaging

| Component | Technology | Version | Rationale |
|-----------|------------|---------|-----------|
| **Database** | PostgreSQL | 18 | Advanced JSONB support and robust extension ecosystem. |
| **Messaging** | pgmq | 1.8.0 | Simplicity of using the existing database for robust asynchronous queuing. |
| **Migrations** | Liquibase | 4.30 | Database-agnostic, versioned schema management. |

## WebAssembly (FaaS Engine)

| Component | Technology | Version | Rationale |
|-----------|------------|---------|-----------|
| **WASM Runtime** | Chicory | 1.6.1 | Pure Java implementation, avoiding native JNI overhead and complexity. |
| **Compiler** | AssemblyScript | Latest | TypeScript-like syntax for easy developer onboarding to WASM. |

## Infrastructure

| Component | Technology | Version | Rationale |
|-----------|------------|---------|-----------|
| **Containerization** | Podman | Latest | Daemon-less, rootless alternative to Docker. |
| **Orchestration** | Podman Compose | Latest | Local multi-container development environment. |

## Library Versions (libs.versions.toml)

Refer to `/projectNIL/gradle/libs.versions.toml` for exact build-time dependencies.

- `springBoot`: 4.0.0
- `junit`: 5.11.4
- `chicory`: 1.6.1
- `postgresql`: Driver compatible with PG 18
- `liquibase`: 4.30.0
