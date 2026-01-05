# Tutorial

A hands-on walkthrough of ProjectNIL's core workflow: register a function, compile it, execute it, and view results.

## Prerequisites

- `make setup` completed (postgres + pgmq container running)
- `make api` running in terminal 1
- `make compiler` running in terminal 2
- AssemblyScript compiler (`asc`) installed locally:
  ```bash
  npm install -g assemblyscript
  asc --version  # verify installation
  ```

## 1. Health Check

Verify the API is running:

```bash
curl http://localhost:8080/health
```

Expected: `{"status":"UP"}`

## 2. Register an Echo Function

Start with a simple `echo` function that returns its input unchanged:

```bash
curl -X POST http://localhost:8080/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "echo",
    "description": "Returns input unchanged",
    "language": "assemblyscript",
    "source": "export function handle(input: string): string { return input; }"
  }'
```

Response:

```json
{
  "id": "a1b2c3d4-...",
  "name": "echo",
  "status": "PENDING",
  "createdAt": "2025-01-05T10:00:00Z"
}
```

Copy the `id` for the next steps.

## 3. Check Compilation Status

Poll until `status` is `READY`:

```bash
curl http://localhost:8080/functions/{id}
```

Once compiled:

```json
{
  "id": "a1b2c3d4-...",
  "name": "echo",
  "status": "READY",
  ...
}
```

If `status` is `FAILED`, check the `compileError` field.

## 4. Execute the Function

```bash
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"message": "Hello, World!"}}'
```

Response:

```json
{
  "id": "e5f6g7h8-...",
  "functionId": "a1b2c3d4-...",
  "status": "COMPLETED",
  "output": {"message": "Hello, World!"},
  "errorMessage": null,
  "createdAt": "2025-01-05T10:01:00Z"
}
```

Copy the execution `id` to view details.

## 5. View Execution Details

```bash
curl http://localhost:8080/executions/{id}
```

Returns full execution details including input, output, and timestamps.

## 6. List All Executions

View all executions for a function:

```bash
curl http://localhost:8080/functions/{id}/executions
```

Returns a list of execution summaries for the given function.

## 7. Register a Function with JSON Processing

Now register a `greet` function that parses input and returns a greeting:

```bash
curl -X POST http://localhost:8080/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "greet",
    "description": "Returns a greeting",
    "language": "assemblyscript",
    "source": "import { JSON } from \"json-as\"; export function handle(input: string): string { const data = JSON.parse<Map<string, string>>(input); const name = data.get(\"name\"); return JSON.stringify<Map<string, string>>(new Map<string, string>().set(\"message\", \"Hello, \" + name + \"!\")); }"
  }'
```

Wait for compilation (`status: READY`),

```
curl http://localhost:8080/functions/{id}
```

then execute:

```bash
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"name": "World"}}'
```

Expected output:

```json
{
  "status": "COMPLETED",
  "output": {"message": "Hello, World!"},
  ...
}
```
