# API Reference

Base URL: `http://localhost:8080`

## Endpoints

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

---

## Functions

### Register a Function

```
POST /functions
```

```json
{
  "name": "add",
  "description": "Adds two numbers",
  "language": "assemblyscript",
  "source": "export function add(a: i32, b: i32): i32 { return a + b; }"
}
```

**Response** `201 Created`:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "add",
  "status": "PENDING",
  "createdAt": "2025-12-18T10:00:00Z"
}
```

### Get Function

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
  "source": "export function add(a: i32, b: i32): i32 { return a + b; }",
  "status": "READY",
  "compileError": null,
  "createdAt": "2025-12-18T10:00:00Z",
  "updatedAt": "2025-12-18T10:00:05Z"
}
```

### Execute a Function

```
POST /functions/{id}/execute
```

```json
{
  "input": { "a": 5, "b": 3 }
}
```

**Response** `200 OK`:
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "functionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "input": { "a": 5, "b": 3 },
  "output": { "result": 8 },
  "startedAt": "2025-12-18T10:01:00Z",
  "completedAt": "2025-12-18T10:01:00Z"
}
```

---

## Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content (successful delete) |
| 400 | Bad Request (e.g., execute non-READY function) |
| 404 | Not Found |
| 500 | Server Error |

## Function Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Source received, awaiting compilation |
| `COMPILING` | Compilation in progress |
| `READY` | Compiled, ready to execute |
| `FAILED` | Compilation failed |

## Execution Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Execution queued |
| `RUNNING` | Execution in progress |
| `COMPLETED` | Execution succeeded |
| `FAILED` | Execution failed |
