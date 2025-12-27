# Getting Started with ProjectNIL

This guide walks you through setting up ProjectNIL and running your first function in under 5 minutes.

## Prerequisites

- [Podman](https://podman.io/) or Docker
- [Podman Compose](https://github.com/containers/podman-compose) or Docker Compose
- Java 25+ (for running services locally)
- curl (for API calls)

## Step 1: Start the Infrastructure

```bash
cd projectNIL/infra

# Start PostgreSQL with pgmq extension
podman compose up -d postgres

# Run database migrations
podman compose --profile migrate up liquibase

# Start the compiler service
podman compose --profile full up -d compiler
```

Verify everything is running:
```bash
podman compose ps
```

You should see `projectnil-db` and `projectnil-compiler` containers running.

## Step 2: Start the API Service

In a new terminal:
```bash
cd projectNIL
./gradlew :services:api:bootRun
```

The API will start on `http://localhost:8080`.

Verify with a health check:
```bash
curl http://localhost:8080/health
```

Expected response:
```json
{"status":"UP"}
```

## Step 3: Register Your First Function

Create a simple function that adds two numbers:

```bash
curl -X POST http://localhost:8080/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "add",
    "description": "Adds two numbers",
    "language": "assemblyscript",
    "source": "export function handle(input: string): string { const data = JSON.parse(input); const sum = (data.a as i32) + (data.b as i32); return JSON.stringify({ sum: sum }); }"
  }'
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add",
  "status": "PENDING",
  "createdAt": "2025-12-27T10:00:00Z"
}
```

The function status is `PENDING` while it's being compiled.

## Step 4: Wait for Compilation

The compiler service will pick up the job and compile your AssemblyScript code to WebAssembly. This usually takes 2-5 seconds.

Check the function status:
```bash
curl http://localhost:8080/functions/{id}
```

Wait until `status` changes to `READY`:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add",
  "status": "READY",
  ...
}
```

If compilation fails, `status` will be `FAILED` and `compileError` will contain the error message.

## Step 5: Execute the Function

Now execute your function with some input:

```bash
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{
    "input": { "a": 5, "b": 3 }
  }'
```

Response:
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "output": { "sum": 8 },
  "errorMessage": null,
  "createdAt": "2025-12-27T10:01:00Z"
}
```

Your function executed successfully and returned `{ "sum": 8 }`.

## Step 6: View Execution History

List all executions for your function:
```bash
curl http://localhost:8080/functions/{id}/executions
```

Get details for a specific execution:
```bash
curl http://localhost:8080/executions/{execution-id}
```

## What's Next?

- **[Writing Functions Guide](./writing-functions.md)** - Learn how to write more complex AssemblyScript functions
- **[API Reference](../api.md)** - Complete endpoint documentation
- **[Error Handling](#error-handling)** - Understanding error responses

---

## Troubleshooting

### Function stays in PENDING

Check if the compiler service is running:
```bash
podman compose logs compiler
```

Check the compilation queue:
```bash
podman exec projectnil-db psql -U projectnil -d projectnil -c \
  "SELECT * FROM pgmq.read('compilation_jobs', 30, 10);"
```

### Compilation fails

Check the `compileError` field in the function response. Common issues:
- Syntax errors in AssemblyScript code
- Missing `handle` export function
- Missing JSON import

### Execution fails with "Function not ready"

The function must be in `READY` status before execution. Check the function status:
```bash
curl http://localhost:8080/functions/{id}
```

If `status` is `FAILED`, fix the source code and update the function:
```bash
curl -X PUT http://localhost:8080/functions/{id} \
  -H "Content-Type: application/json" \
  -d '{ "name": "add", "language": "assemblyscript", "source": "..." }'
```

### Database connection issues

Ensure PostgreSQL is running:
```bash
podman compose ps
```

Check the database logs:
```bash
podman compose logs postgres
```

---

## Quick Reference

```bash
# Start infrastructure
podman compose up -d postgres
podman compose --profile migrate up liquibase
podman compose --profile full up -d compiler

# Start API
./gradlew :services:api:bootRun

# Register function
curl -X POST http://localhost:8080/functions -H "Content-Type: application/json" -d '...'

# Check function status
curl http://localhost:8080/functions/{id}

# Execute function
curl -X POST http://localhost:8080/functions/{id}/execute -H "Content-Type: application/json" -d '{"input": {...}}'

# Stop everything
podman compose --profile full down
```
