# ProjectNIL

A Function-as-a-Service (FaaS) implementation powered by WebAssembly (WASM) with multi-language support.

## Overview

ProjectNIL enables serverless function execution using WebAssembly as the runtime. This provides a secure, sandboxed environment for running user-defined functions with near-native performance.

### Supported Languages

- **AssemblyScript** - Currently supported
- More languages coming soon

## Development Setup

### Prerequisites

- Docker or Podman with compose support

### Start Dependencies (Database + Migrations)

```bash
# Start PostgreSQL and run migrations
podman compose up -d
```

> **Note:** Use `podman compose` (native, with a space) rather than `podman-compose` (Python wrapper).
> The native version is idempotent and handles existing containers correctly.

This will:
1. Start a PostgreSQL database (with PGMQ extension) on port 5432
2. Run Liquibase migrations to set up the schema

### Default Database Credentials

| Setting  | Value       |
|----------|-------------|
| Host     | localhost   |
| Port     | 5432        |
| Database | projectnil  |
| User     | projectnil  |
| Password | projectnil  |

### Connect to Database

```bash
podman exec -it projectnil-db psql -U projectnil -d projectnil
```

### Running Services

```bash
# Run the API service (port 8080)
./gradlew :services:api:bootRun

# Run the Compiler service (port 8081)
./gradlew :services:compiler:bootRun
```
