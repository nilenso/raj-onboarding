# Production Testing Observations

**Date**: December 27, 2025  
**Production URL**: http://157.245.108.179:8080  
**Tester**: Claude (following Getting Started guide)

---

## Executive Summary

**Production is NOT functional for the core use case.**

The primary workflow (register → compile → execute) is completely blocked because `POST /functions` returns 500. Read-only operations work, but all write operations and error handling have issues.

### Priority Bugs

| Priority | Bug | Impact |
|----------|-----|--------|
| **P0** | POST /functions returns 500 | Core functionality broken |
| **P1** | Framework exceptions return 500 | Poor DX, hard to debug |
| **P2** | Invalid UUID returns 500 | Should be 400 |
| **P3** | Invalid JSON/empty body returns 500 | Should be 400 |

### Immediate Actions Required

1. **SSH into production** and check:
   - API logs: `podman logs projectnil-api`
   - PGMQ queues exist: `SELECT pgmq.list_queues();`
   - Migrations ran: `podman logs projectnil-migrations`

2. **Fix GlobalExceptionHandler** to properly handle:
   - `NoResourceFoundException` → 404
   - `HttpRequestMethodNotSupportedException` → 405
   - `MethodArgumentTypeMismatchException` → 400
   - `HttpMessageNotReadableException` → 400

---

## Test Summary

| Test | Expected | Actual | Status |
|------|----------|--------|--------|
| Health check | 200 UP | 200 UP | PASS |
| List functions | 200 [] | 200 [] | PASS |
| Get non-existent function (valid UUID) | 404 | 404 | PASS |
| Get non-existent execution (valid UUID) | 404 | 404 | PASS |
| Unsupported language validation | 415 | 415 | PASS |
| **Register function** | **201** | **500** | **FAIL** |
| Get function (invalid UUID) | 400 | 500 | FAIL |
| Non-existent endpoint | 404 | 500 | FAIL |
| Wrong HTTP method | 405 | 500 | FAIL |
| Empty request body | 400 | 500 | FAIL |
| Invalid JSON | 400 | 500 | FAIL |

---

## Bug #1: POST /functions returns 500 Internal Server Error

### Steps to Reproduce

```bash
export API_URL="http://157.245.108.179:8080"

curl -X POST $API_URL/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "add",
    "description": "Adds two numbers",
    "language": "assemblyscript",
    "source": "export function handle(input: string): string { return input; }"
  }'
```

### Expected Response

```json
{
  "id": "...",
  "name": "add",
  "status": "PENDING",
  "createdAt": "..."
}
```

### Actual Response

```json
{
  "timestamp": "2025-12-27T07:40:02.610285242",
  "message": "Internal server error",
  "error": "Internal Server Error",
  "status": 500
}
```

### Analysis

1. **Validation works**: Unsupported language returns proper 415 error
2. **Read operations work**: GET /functions, GET /functions/{id} work correctly
3. **Failure point**: Error occurs after validation, likely during:
   - Database save (Function entity)
   - PGMQ queue publish (CompilationJob)

### Likely Root Causes

1. **PGMQ queue not initialized**: The `compilation_jobs` queue may not exist in production
2. **Database migration incomplete**: PGMQ extension or queues not created
3. **PGMQ connection issue**: API can't connect to queue

### Recommended Investigation

1. SSH into production and check:
   ```bash
   podman exec projectnil-db psql -U projectnil -d projectnil -c "\dx"
   # Should show pgmq extension
   
   podman exec projectnil-db psql -U projectnil -d projectnil -c "SELECT pgmq.list_queues();"
   # Should show compilation_jobs, compilation_results
   ```

2. Check API logs:
   ```bash
   podman logs projectnil-api --tail 50
   ```

3. Check if migrations ran:
   ```bash
   podman logs projectnil-migrations
   ```

---

## Observations: What Works

### 1. Health Check
```bash
curl http://157.245.108.179:8080/health
# {"status":"UP"}
```

### 2. List Functions (empty)
```bash
curl http://157.245.108.179:8080/functions
# []
```

### 3. 404 for Non-existent Resources
```bash
curl http://157.245.108.179:8080/functions/00000000-0000-0000-0000-000000000000
# {"timestamp":"...","message":"Function not found: ...","error":"Not Found","status":404}
```

### 4. Language Validation
```bash
curl -X POST http://157.245.108.179:8080/functions \
  -H "Content-Type: application/json" \
  -d '{"name":"x","language":"rust","source":"x"}'
# {"timestamp":"...","message":"Unsupported language: rust. Supported: [assemblyscript]","error":"Unsupported Media Type","status":415}
```

---

## Bug #2: Missing/Invalid Request Body Returns 500 Instead of 400

### Observations

| Request Issue | Expected | Actual |
|---------------|----------|--------|
| Missing Content-Type header | 415 or 400 | 500 |
| Empty request body | 400 | 500 |
| Invalid JSON `{invalid}` | 400 | 500 |
| Missing required fields | 400 | 415 (language validation first) |

### Examples

```bash
# Empty body - should be 400
curl -X POST http://157.245.108.179:8080/functions \
  -H "Content-Type: application/json" -d ''
# Returns 500

# Invalid JSON - should be 400
curl -X POST http://157.245.108.179:8080/functions \
  -H "Content-Type: application/json" -d '{invalid}'
# Returns 500
```

### Impact

- Poor developer experience - unhelpful error messages
- Security concern - 500 errors may leak stack traces in logs

### Recommendation

Add proper request validation and JSON parsing error handling in `GlobalExceptionHandler`:
- `HttpMessageNotReadableException` → 400
- `MissingServletRequestParameterException` → 400
- `MethodArgumentTypeMismatchException` → 400

---

## Bug #3: Invalid UUID Path Parameter Returns 500

### Steps to Reproduce

```bash
curl -X POST http://157.245.108.179:8080/functions/not-a-uuid/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {}}'
```

### Expected

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid UUID format: not-a-uuid"
}
```

### Actual

```json
{
  "timestamp": "...",
  "message": "Internal server error",
  "error": "Internal Server Error",
  "status": 500
}
```

### Affected Endpoints

- `GET /functions/{id}` with invalid UUID
- `PUT /functions/{id}` with invalid UUID  
- `DELETE /functions/{id}` with invalid UUID
- `POST /functions/{id}/execute` with invalid UUID
- `GET /functions/{id}/executions` with invalid UUID
- `GET /executions/{id}` with invalid UUID

### Recommendation

Add `MethodArgumentTypeMismatchException` handler to `GlobalExceptionHandler`:

```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String message = String.format("Invalid %s: %s", ex.getName(), ex.getValue());
    return ResponseEntity.badRequest().body(new ErrorResponse(400, "Bad Request", message));
}
```

---

## Bug #4: 404 and 405 Errors Return 500

### Observations

```bash
# Non-existent endpoint - should be 404
curl http://157.245.108.179:8080/nonexistent
# Returns 500

# Wrong HTTP method - should be 405
curl -X PATCH http://157.245.108.179:8080/functions  
# Returns 500
```

### Impact

- All Spring MVC framework exceptions are being caught by generic handler
- Makes debugging very difficult
- Poor API experience

### Root Cause Theory

The `GlobalExceptionHandler` likely has a catch-all `@ExceptionHandler(Exception.class)` that's swallowing specific exceptions like:
- `NoResourceFoundException` (Spring 6.x) → should be 404
- `HttpRequestMethodNotSupportedException` → should be 405
- `MethodArgumentTypeMismatchException` → should be 400

### Recommendation

Add specific exception handlers BEFORE the generic catch-all:

```java
@ExceptionHandler(NoResourceFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
    return ResponseEntity.status(404).body(...);
}

@ExceptionHandler(HttpRequestMethodNotSupportedException.class)  
public ResponseEntity<ErrorResponse> handleMethodNotAllowed(...) {
    return ResponseEntity.status(405).body(...);
}
```

---

## Observation: Input Validation Order

The execute endpoint validates function existence before input format:

```bash
# With invalid input but non-existent function
curl -X POST $API_URL/functions/00000000.../execute -d '{"input": "string"}'
# Returns 404 (function not found), not 400 (invalid input)
```

This is acceptable behavior (fail-fast on existence) but worth noting:
- Input validation happens inside `ExecutionService.execute()` after function lookup
- A truly invalid request with bad input AND non-existent function returns 404

---

## Unable to Test (Blocked by Bug #1)

Since we can't create functions, the following cannot be tested:

- [ ] GET /functions/{id} (with valid function)
- [ ] PUT /functions/{id}
- [ ] DELETE /functions/{id}
- [ ] POST /functions/{id}/execute
- [ ] GET /functions/{id}/executions
- [ ] GET /executions/{id}
- [ ] Compilation flow (PENDING → COMPILING → READY)
- [ ] Execution flow

---

## Documentation Issues Found

### Issue #1: Getting Started guide assumes local setup works

The guide jumps straight to `curl http://localhost:8080/functions` without verifying the full stack is operational. Consider adding a "verify your setup" section.

### Issue #2: No production troubleshooting section

The guide has troubleshooting for local development but not for production issues.

### Issue #3: Error messages don't help diagnose

The 500 error says "Internal server error" with no actionable details. Consider:
- Adding correlation IDs to error responses
- Logging more context in production logs

---

## Next Steps

1. **Immediate**: Investigate and fix Bug #1 (500 on POST /functions)
2. **After fix**: Complete the testing of all endpoints
3. **Documentation**: Add production troubleshooting guide
4. **Observability**: Add correlation IDs and structured logging
