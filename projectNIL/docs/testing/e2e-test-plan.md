# End-to-End Test Plan

**Issue**: [#74](https://github.com/nilenso/raj-onboarding/issues/74)  
**Last Updated**: December 27, 2025

This document defines all test scenarios for validating ProjectNIL API functionality.

---

## Test Environment

```bash
# Set target environment
export API_URL="http://localhost:8080"        # Local development
export API_URL="http://157.245.108.179:8080"  # Production
```

---

## Test Categories

| Category | Tests | Priority |
|----------|-------|----------|
| 1. Health & Infrastructure | 2 | Smoke |
| 2. Function CRUD | 15 | Core |
| 3. Function Lifecycle | 6 | Core |
| 4. Execution | 12 | Core |
| 5. Error Handling | 15 | Regression |
| 6. Edge Cases | 10 | Comprehensive |
| **Total** | **60** | |

---

## 1. Health & Infrastructure

### 1.1 Health Check
```bash
curl $API_URL/health
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body | `{"status":"UP"}` |

### 1.2 Database Connectivity (implicit)
```bash
curl $API_URL/functions
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body | `[]` or array of functions |

---

## 2. Function CRUD

### 2.1 Create Function - Success
```bash
curl -X POST $API_URL/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-add",
    "description": "Test function",
    "language": "assemblyscript",
    "source": "export function handle(input: string): string { return input; }"
  }'
```
| Field | Expected |
|-------|----------|
| Status | 201 |
| Body.id | UUID format |
| Body.name | "test-add" |
| Body.status | "PENDING" |
| Body.createdAt | ISO timestamp |

### 2.2 Create Function - Minimal (no description)
```bash
curl -X POST $API_URL/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "minimal",
    "language": "assemblyscript",
    "source": "export function handle(input: string): string { return input; }"
  }'
```
| Field | Expected |
|-------|----------|
| Status | 201 |
| Body.description | null |

### 2.3 Create Function - Duplicate Name
```bash
# Create first
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "duplicate", "language": "assemblyscript", "source": "..."}'
# Create second with same name
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "duplicate", "language": "assemblyscript", "source": "..."}'
```
| Field | Expected |
|-------|----------|
| Status | 409 or 201 (TBD - is name unique?) |

### 2.4 Create Function - Unsupported Language
```bash
curl -X POST $API_URL/functions \
  -H "Content-Type: application/json" \
  -d '{"name": "rust-fn", "language": "rust", "source": "fn main() {}"}'
```
| Field | Expected |
|-------|----------|
| Status | 415 |
| Body.message | Contains "Unsupported language: rust" |

### 2.5 List Functions - Empty
```bash
curl $API_URL/functions
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body | `[]` |

### 2.6 List Functions - With Data
```bash
# After creating functions
curl $API_URL/functions
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body | Array of FunctionResponse |
| Each item | id, name, status, createdAt |

### 2.7 Get Function - Exists
```bash
curl $API_URL/functions/{id}
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body.id | Matches request |
| Body | All fields: id, name, description, language, source, status, compileError, createdAt, updatedAt |

### 2.8 Get Function - Not Found
```bash
curl $API_URL/functions/00000000-0000-0000-0000-000000000000
```
| Field | Expected |
|-------|----------|
| Status | 404 |
| Body.message | "Function not found: ..." |

### 2.9 Update Function - Name Only
```bash
curl -X PUT $API_URL/functions/{id} \
  -H "Content-Type: application/json" \
  -d '{"name": "renamed", "language": "assemblyscript", "source": "..."}'
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body.name | "renamed" |
| Body.status | Unchanged if source/language same |

### 2.10 Update Function - Source Changed (Recompile)
```bash
curl -X PUT $API_URL/functions/{id} \
  -H "Content-Type: application/json" \
  -d '{"name": "fn", "language": "assemblyscript", "source": "NEW SOURCE"}'
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body.status | "PENDING" |
| Body.compileError | null |

### 2.11 Update Function - Not Found
```bash
curl -X PUT $API_URL/functions/00000000-0000-0000-0000-000000000000 \
  -H "Content-Type: application/json" \
  -d '{"name": "x", "language": "assemblyscript", "source": "x"}'
```
| Field | Expected |
|-------|----------|
| Status | 404 |

### 2.12 Delete Function - Exists
```bash
curl -X DELETE $API_URL/functions/{id}
```
| Field | Expected |
|-------|----------|
| Status | 204 |
| Body | Empty |

### 2.13 Delete Function - Not Found
```bash
curl -X DELETE $API_URL/functions/00000000-0000-0000-0000-000000000000
```
| Field | Expected |
|-------|----------|
| Status | 404 |

### 2.14 Delete Function - Verify Cascade
```bash
# Create function, execute it, then delete
curl -X DELETE $API_URL/functions/{id}
# Verify executions are also deleted
curl $API_URL/executions/{execution-id}
```
| Field | Expected |
|-------|----------|
| Delete Status | 204 |
| Execution Status | 404 |

### 2.15 Get Function After Delete
```bash
curl $API_URL/functions/{deleted-id}
```
| Field | Expected |
|-------|----------|
| Status | 404 |

---

## 3. Function Lifecycle

### 3.1 PENDING → COMPILING → READY (Happy Path)
```bash
# Create function
RESPONSE=$(curl -s -X POST $API_URL/functions ...)
ID=$(echo $RESPONSE | jq -r '.id')
echo "Status: $(echo $RESPONSE | jq -r '.status')"  # PENDING

# Wait and poll
sleep 5
curl $API_URL/functions/$ID | jq '.status'  # COMPILING or READY

# Final state
sleep 10
curl $API_URL/functions/$ID | jq '.status'  # READY
```
| Stage | Expected Status |
|-------|-----------------|
| Immediately | PENDING |
| After ~2s | COMPILING |
| After ~5s | READY |

### 3.2 PENDING → COMPILING → FAILED (Compile Error)
```bash
curl -X POST $API_URL/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "bad-syntax",
    "language": "assemblyscript",
    "source": "this is not valid assemblyscript {{{"
  }'
```
| Stage | Expected |
|-------|----------|
| Final status | FAILED |
| compileError | Contains error message |
| wasmBinary | null |

### 3.3 FAILED → PENDING (Fix and Recompile)
```bash
# Update with valid source
curl -X PUT $API_URL/functions/{failed-id} \
  -H "Content-Type: application/json" \
  -d '{"name": "fixed", "language": "assemblyscript", "source": "VALID SOURCE"}'
```
| Field | Expected |
|-------|----------|
| Status after update | PENDING |
| compileError | null |

### 3.4 READY → PENDING (Source Updated)
```bash
# Update source of READY function
curl -X PUT $API_URL/functions/{ready-id} ...
```
| Field | Expected |
|-------|----------|
| Status | PENDING |

### 3.5 Compilation Timeout
```bash
# Create function with infinite loop in compilation (if possible)
# Or verify timeout behavior
```
| Field | Expected |
|-------|----------|
| Status | FAILED after timeout |
| compileError | Timeout message |

### 3.6 Compiler Service Down
```bash
# Stop compiler, create function
# Function should stay PENDING indefinitely
```
| Field | Expected |
|-------|----------|
| Status | PENDING (no transition) |

---

## 4. Execution

### 4.1 Execute READY Function - Success
```bash
curl -X POST $API_URL/functions/{ready-id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"a": 5, "b": 3}}'
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body.status | "COMPLETED" |
| Body.output | Parsed JSON object |
| Body.errorMessage | null |

### 4.2 Execute - Echo Input
```bash
# Function: export function handle(input: string): string { return input; }
curl -X POST $API_URL/functions/{echo-id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"message": "hello"}}'
```
| Field | Expected |
|-------|----------|
| Body.output | `{"message": "hello"}` |

### 4.3 Execute - Empty Input (defaults to {})
```bash
curl -X POST $API_URL/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{}'
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Execution input | `{}` |

### 4.4 Execute - Null Input (defaults to {})
```bash
curl -X POST $API_URL/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": null}'
```
| Field | Expected |
|-------|----------|
| Status | 200 |

### 4.5 Execute - Invalid Input (Primitive)
```bash
curl -X POST $API_URL/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": "string"}'
```
| Field | Expected |
|-------|----------|
| Status | 400 |
| Body.message | "Input must be a JSON object" |

### 4.6 Execute - Invalid Input (Array)
```bash
curl -X POST $API_URL/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": [1, 2, 3]}'
```
| Field | Expected |
|-------|----------|
| Status | 400 |

### 4.7 Execute PENDING Function
```bash
curl -X POST $API_URL/functions/{pending-id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {}}'
```
| Field | Expected |
|-------|----------|
| Status | 400 |
| Body.message | "Function ... is not ready" |

### 4.8 Execute FAILED Function
```bash
curl -X POST $API_URL/functions/{failed-id}/execute ...
```
| Field | Expected |
|-------|----------|
| Status | 400 |
| Body.message | Contains status info |

### 4.9 Execute Non-existent Function
```bash
curl -X POST $API_URL/functions/00000000-0000-0000-0000-000000000000/execute ...
```
| Field | Expected |
|-------|----------|
| Status | 404 |

### 4.10 Execute - Runtime Error (User Code)
```bash
# Function that throws: throw new Error("oops")
curl -X POST $API_URL/functions/{error-fn}/execute ...
```
| Field | Expected |
|-------|----------|
| HTTP Status | 200 (not 500!) |
| Body.status | "FAILED" |
| Body.errorMessage | Contains error |
| Body.output | null |

### 4.11 Execute - Timeout
```bash
# Function with infinite loop
curl -X POST $API_URL/functions/{infinite-loop}/execute ...
```
| Field | Expected |
|-------|----------|
| HTTP Status | 200 |
| Body.status | "FAILED" |
| Body.errorMessage | Timeout message |

### 4.12 List Executions for Function
```bash
curl $API_URL/functions/{id}/executions
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Body | Array of ExecutionSummaryResponse |
| Each item | id, status, startedAt, completedAt |
| Order | Newest first |

---

## 5. Error Handling

### 5.1 404 - Non-existent Endpoint
```bash
curl $API_URL/nonexistent
```
| Field | Expected |
|-------|----------|
| Status | 404 |
| Body.error | "Not Found" |

### 5.2 405 - Method Not Allowed
```bash
curl -X PATCH $API_URL/functions
```
| Field | Expected |
|-------|----------|
| Status | 405 |
| Body.error | "Method Not Allowed" |

### 5.3 400 - Invalid UUID Format
```bash
curl $API_URL/functions/not-a-uuid
```
| Field | Expected |
|-------|----------|
| Status | 400 |
| Body.message | Contains "Invalid" |

### 5.4 400 - Empty Request Body
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" -d ''
```
| Field | Expected |
|-------|----------|
| Status | 400 |

### 5.5 400 - Invalid JSON
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" -d '{invalid}'
```
| Field | Expected |
|-------|----------|
| Status | 400 |
| Body.message | Contains "JSON" |

### 5.6 415 - Missing Content-Type
```bash
curl -X POST $API_URL/functions -d '{"name":"x"}'
```
| Field | Expected |
|-------|----------|
| Status | 415 or 400 |

### 5.7 415 - Wrong Content-Type
```bash
curl -X POST $API_URL/functions -H "Content-Type: text/plain" -d 'hello'
```
| Field | Expected |
|-------|----------|
| Status | 415 |

### 5.8 400 - Missing Required Field (name)
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"language": "assemblyscript", "source": "x"}'
```
| Field | Expected |
|-------|----------|
| Status | 400 |
| Body.message | Contains "name" |

### 5.9 400 - Missing Required Field (source)
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "x", "language": "assemblyscript"}'
```
| Field | Expected |
|-------|----------|
| Status | 400 |

### 5.10 400 - Missing Required Field (language)
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "x", "source": "x"}'
```
| Field | Expected |
|-------|----------|
| Status | 415 (validates language first) |

### 5.11 Error Response Format Consistency
All error responses should have:
```json
{
  "timestamp": "ISO-8601",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive message"
}
```

### 5.12 404 - Get Execution Not Found
```bash
curl $API_URL/executions/00000000-0000-0000-0000-000000000000
```
| Field | Expected |
|-------|----------|
| Status | 404 |

### 5.13 404 - List Executions for Non-existent Function
```bash
curl $API_URL/functions/00000000-0000-0000-0000-000000000000/executions
```
| Field | Expected |
|-------|----------|
| Status | 404 |

### 5.14 400 - Invalid Execution ID Format
```bash
curl $API_URL/executions/invalid
```
| Field | Expected |
|-------|----------|
| Status | 400 |

### 5.15 Concurrent Execution Safety
```bash
# Execute same function 10 times concurrently
for i in {1..10}; do
  curl -X POST $API_URL/functions/{id}/execute -d '{"input":{}}' &
done
wait
```
| Field | Expected |
|-------|----------|
| All requests | 200 |
| No data corruption | Verify in DB |

---

## 6. Edge Cases

### 6.1 Very Long Function Name
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d "{\"name\": \"$(printf 'a%.0s' {1..1000})\", \"language\": \"assemblyscript\", \"source\": \"x\"}"
```
| Field | Expected |
|-------|----------|
| Status | 400 or 201 (depends on limit) |

### 6.2 Very Long Source Code
```bash
# 1MB source code
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d "{\"name\": \"big\", \"language\": \"assemblyscript\", \"source\": \"$(printf 'x%.0s' {1..1000000})\"}"
```
| Field | Expected |
|-------|----------|
| Status | 201 or 413 |

### 6.3 Unicode in Function Name
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "函数-🚀", "language": "assemblyscript", "source": "x"}'
```
| Field | Expected |
|-------|----------|
| Status | 201 |
| Body.name | "函数-🚀" |

### 6.4 Unicode in Source Code
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "unicode", "language": "assemblyscript", "source": "// 你好世界"}'
```
| Field | Expected |
|-------|----------|
| Status | 201 |

### 6.5 Special Characters in Input
```bash
curl -X POST $API_URL/functions/{id}/execute -H "Content-Type: application/json" \
  -d '{"input": {"text": "Hello \"World\" \n\t\\"}}'
```
| Field | Expected |
|-------|----------|
| Status | 200 |
| Output | Properly escaped |

### 6.6 Large Input Object
```bash
# 1MB input
curl -X POST $API_URL/functions/{id}/execute -H "Content-Type: application/json" \
  -d "{\"input\": {\"data\": \"$(printf 'x%.0s' {1..1000000})\"}}"
```
| Field | Expected |
|-------|----------|
| Status | 200 or 413 |

### 6.7 Deeply Nested Input
```bash
# 100 levels deep
curl -X POST $API_URL/functions/{id}/execute -H "Content-Type: application/json" \
  -d '{"input": {"a":{"a":{"a":{"a":...}}}}}'
```
| Field | Expected |
|-------|----------|
| Status | 200 or 400 |

### 6.8 Empty String Values
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "", "language": "assemblyscript", "source": ""}'
```
| Field | Expected |
|-------|----------|
| Status | 400 (name required) |

### 6.9 Null Field Values
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": null, "language": "assemblyscript", "source": "x"}'
```
| Field | Expected |
|-------|----------|
| Status | 400 |

### 6.10 Extra Unknown Fields (Ignored)
```bash
curl -X POST $API_URL/functions -H "Content-Type: application/json" \
  -d '{"name": "x", "language": "assemblyscript", "source": "x", "unknown": "ignored"}'
```
| Field | Expected |
|-------|----------|
| Status | 201 |
| Unknown field | Ignored |

---

## Test Execution Checklist

### Smoke Tests (Run First)
- [ ] 1.1 Health Check
- [ ] 1.2 Database Connectivity
- [ ] 2.1 Create Function
- [ ] 4.1 Execute Function

### Core Tests (Run After Smoke)
- [ ] All Function CRUD (2.x)
- [ ] All Lifecycle (3.x)
- [ ] All Execution (4.x)

### Regression Tests (Run After Fixes)
- [ ] All Error Handling (5.x)

### Comprehensive Tests (Run Before Release)
- [ ] All Edge Cases (6.x)

---

## Test Data Fixtures

### Echo Function (simplest)
```javascript
export function handle(input: string): string {
  return input;
}
```

### Add Function
```javascript
export function handle(input: string): string {
  const data = JSON.parse(input);
  const sum = (data.a as i32) + (data.b as i32);
  return JSON.stringify({ sum: sum });
}
```

### Greeting Function
```javascript
export function handle(input: string): string {
  const data = JSON.parse(input);
  const name = data.name as string;
  return JSON.stringify({ greeting: "Hello, " + name + "!" });
}
```

### Error Function (throws)
```javascript
export function handle(input: string): string {
  throw new Error("Intentional error");
}
```

### Invalid Syntax (compile error)
```javascript
this is not valid {{ assemblyscript
```

### Infinite Loop (timeout test)
```javascript
export function handle(input: string): string {
  while (true) {}
  return input;
}
```
