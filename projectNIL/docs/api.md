# API Reference

Canonical API contracts live in `projectNIL/scope/contracts.md`.

## Base URL

| Environment | URL |
|-------------|-----|
| **Production** | `{{ PRODUCTION_API_URL }}` |
| **Local** | `http://localhost:8080` |

> Set `API_URL` environment variable to your target environment before running examples.

---

## Endpoints Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/functions` | Register a function |
| GET | `/functions` | List all functions |
| GET | `/functions/{id}` | Get function details |
| PUT | `/functions/{id}` | Update a function |
| DELETE | `/functions/{id}` | Delete a function |
| POST | `/functions/{id}/execute` | Execute a function |
| GET | `/functions/{id}/executions` | List executions for a function |
| GET | `/executions/{id}` | Get execution details |
| GET | `/health` | Health check |

---

## Functions

### Register a Function

Creates a new function and triggers compilation.

```
POST /functions
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "add",
  "description": "Adds two numbers",
  "language": "assemblyscript",
  "source": "export function handle(input: string): string { ... }"
}
```

**Response** `201 Created`:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add",
  "status": "PENDING",
  "createdAt": "2025-12-27T10:00:00Z"
}
```

### List All Functions

Returns a lightweight list of all functions.

```
GET /functions
```

**Response** `200 OK`:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "add",
    "status": "READY",
    "createdAt": "2025-12-27T10:00:00Z"
  }
]
```

### Get Function Details

Returns full function details including source code.

```
GET /functions/{id}
```

**Response** `200 OK`:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add",
  "description": "Adds two numbers",
  "language": "assemblyscript",
  "source": "export function handle(input: string): string { ... }",
  "status": "READY",
  "compileError": null,
  "createdAt": "2025-12-27T10:00:00Z",
  "updatedAt": "2025-12-27T10:00:05Z"
}
```

### Update a Function

Updates a function. If source or language changes, triggers recompilation.

```
PUT /functions/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "add-v2",
  "description": "Adds two numbers (improved)",
  "language": "assemblyscript",
  "source": "export function handle(input: string): string { ... }"
}
```

**Response** `200 OK`:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add-v2",
  "description": "Adds two numbers (improved)",
  "language": "assemblyscript",
  "source": "export function handle(input: string): string { ... }",
  "status": "PENDING",
  "compileError": null,
  "createdAt": "2025-12-27T10:00:00Z",
  "updatedAt": "2025-12-27T11:00:00Z"
}
```

### Delete a Function

Deletes a function and all associated executions.

```
DELETE /functions/{id}
```

**Response** `204 No Content`

### Execute a Function

Executes a compiled function with JSON input.

```
POST /functions/{id}/execute
Content-Type: application/json
```

**Request Body:**
```json
{
  "input": { "a": 5, "b": 3 }
}
```

**Response** `200 OK` (success):
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

**Response** `200 OK` (user code error):
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FAILED",
  "output": null,
  "errorMessage": "Runtime error: division by zero",
  "createdAt": "2025-12-27T10:01:00Z"
}
```

> **Note:** User code errors (traps, timeouts) return `200 OK` with `status: FAILED`. Only platform errors return 4xx/5xx.

---

## Executions

### List Executions for a Function

Returns lightweight execution history for a function.

```
GET /functions/{id}/executions
```

**Response** `200 OK`:
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "status": "COMPLETED",
    "startedAt": "2025-12-27T10:01:00Z",
    "completedAt": "2025-12-27T10:01:00Z"
  }
]
```

### Get Execution Details

Returns full execution details including input/output.

```
GET /executions/{id}
```

**Response** `200 OK`:
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "input": { "a": 5, "b": 3 },
  "output": { "sum": 8 },
  "errorMessage": null,
  "startedAt": "2025-12-27T10:01:00Z",
  "completedAt": "2025-12-27T10:01:00Z",
  "createdAt": "2025-12-27T10:01:00Z"
}
```

---

## Health Check

```
GET /health
```

**Response** `200 OK`:
```json
{
  "status": "UP"
}
```

---

## Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content (successful delete) |
| 400 | Bad Request (invalid input, function not ready) |
| 404 | Not Found |
| 415 | Unsupported Media Type (unsupported language) |
| 500 | Internal Server Error |

---

## Error Response Format

All errors return a consistent JSON format:

```json
{
  "timestamp": "2025-12-27T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Input must be a JSON object"
}
```

---

## Function Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Source received, awaiting compilation |
| `COMPILING` | Compilation in progress |
| `READY` | Compiled, ready to execute |
| `FAILED` | Compilation failed (see `compileError`) |

---

## Execution Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Execution queued (not currently used) |
| `RUNNING` | Execution in progress |
| `COMPLETED` | Execution succeeded |
| `FAILED` | Execution failed (see `errorMessage`) |

---

## Input Validation

- **Input must be a JSON object**: Primitives (strings, numbers, booleans), arrays, and `null` are rejected with `400 Bad Request`.
- **Null input defaults to `{}`**: If input is omitted or null, it is treated as an empty object.

**Valid inputs:**
```json
{ "input": { "a": 1, "b": 2 } }
{ "input": {} }
{ }  // defaults to {}
```

**Invalid inputs:**
```json
{ "input": "hello" }      // 400: primitives not allowed
{ "input": [1, 2, 3] }    // 400: arrays not allowed
{ "input": 42 }           // 400: primitives not allowed
```

---

## Supported Languages

| Language | Status | Compiler |
|----------|--------|----------|
| `assemblyscript` | Supported | Node.js + asc |

Future languages (Phase 1+): Rust, Go, C/C++
