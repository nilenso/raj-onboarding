# Getting Started with ProjectNIL

This guide walks you through running your first function on ProjectNIL.

## API Endpoints

| Environment | Base URL |
|-------------|----------|
| **Production** | `{{ defined from PRODUCTION_API_URL, e.g http://<droplet-ip>:8080 }}` |
| **Local Development** | `http://localhost:8080` |

> **Note**: In production examples below, replace `$API_URL` with your production URL or set it as an environment variable.

---

## Quick Start (Production)

If you have access to a running ProjectNIL instance, you can start immediately:

```bash
# Set your API URL (get this from your admin)
export API_URL="http://your-server:8080"

# Verify the API is running
curl $API_URL/health
```

Then skip to [Step 3: Register Your First Function](#step-3-register-your-first-function).

---

## Local Development Setup

### Prerequisites

- [Podman](https://podman.io/) or Docker
- [Podman Compose](https://github.com/containers/podman-compose) or Docker Compose
- Java 25+ (for running services locally)
- curl (for API calls)

### Step 1: Start the Infrastructure

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

### Step 2: Start the API Service

In a new terminal:
```bash
cd projectNIL
./gradlew :services:api:bootRun
```

The API will start on `http://localhost:8080`.

```bash
# Set the local API URL
export API_URL="http://localhost:8080"

# Verify with a health check
curl $API_URL/health
```

Expected response:
```json
{"status":"UP"}
```

---

## Step 3: Register Your First Function

Create a simple function that adds two numbers:

```bash
curl -X POST $API_URL/functions \
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

Save the function ID:
```bash
export FUNCTION_ID="550e8400-e29b-41d4-a716-446655440000"
```

The function status is `PENDING` while it's being compiled.

## Step 4: Wait for Compilation

The compiler service will pick up the job and compile your AssemblyScript code to WebAssembly. This usually takes 2-5 seconds.

Check the function status:
```bash
curl $API_URL/functions/$FUNCTION_ID
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
curl -X POST $API_URL/functions/$FUNCTION_ID/execute \
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
curl $API_URL/functions/$FUNCTION_ID/executions
```

Get details for a specific execution:
```bash
curl $API_URL/executions/{execution-id}
```

## What's Next?

- **[Writing Functions Guide](./writing-functions.md)** - Learn how to write more complex AssemblyScript functions
- **[API Reference](../api.md)** - Complete endpoint documentation

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
curl $API_URL/functions/$FUNCTION_ID
```

If `status` is `FAILED`, fix the source code and update the function:
```bash
curl -X PUT $API_URL/functions/$FUNCTION_ID \
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
# Set API URL (production or local)
export API_URL="http://localhost:8080"  # or your production URL

# Health check
curl $API_URL/health

# Register function
curl -X POST $API_URL/functions -H "Content-Type: application/json" -d '...'

# Check function status
curl $API_URL/functions/$FUNCTION_ID

# Execute function
curl -X POST $API_URL/functions/$FUNCTION_ID/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {...}}'

# List executions
curl $API_URL/functions/$FUNCTION_ID/executions
```

### Local Development Commands

```bash
# Start infrastructure
podman compose up -d postgres
podman compose --profile migrate up liquibase
podman compose --profile full up -d compiler

# Start API
./gradlew :services:api:bootRun

# Stop everything
podman compose --profile full down
```
