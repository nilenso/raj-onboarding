# Writing Functions for ProjectNIL

This guide explains how to write functions for ProjectNIL using AssemblyScript.

## Overview

ProjectNIL functions are written in AssemblyScript (a TypeScript-like language that compiles to WebAssembly). Each function must export a `handle` function that receives JSON input and returns JSON output.

## The Handle Function

Every function must export a `handle` function with this signature:

```typescript
export function handle(input: string): string
```

- **input**: A JSON string containing the input data
- **returns**: A JSON string containing the output data

## Basic Example: Echo

The simplest function just returns its input:

```typescript
export function handle(input: string): string {
  return input;
}
```

**Input:** `{"message": "hello"}`  
**Output:** `{"message": "hello"}`

## Example: Add Two Numbers

```typescript
export function handle(input: string): string {
  // Parse the JSON input
  const data = JSON.parse(input);
  
  // Extract values (AssemblyScript needs explicit casting)
  const a = data.a as i32;
  const b = data.b as i32;
  
  // Calculate result
  const sum = a + b;
  
  // Return as JSON
  return JSON.stringify({ sum: sum });
}
```

**Input:** `{"a": 5, "b": 3}`  
**Output:** `{"sum": 8}`

## Example: String Manipulation

```typescript
export function handle(input: string): string {
  const data = JSON.parse(input);
  const name = data.name as string;
  
  const greeting = "Hello, " + name + "!";
  
  return JSON.stringify({ greeting: greeting });
}
```

**Input:** `{"name": "World"}`  
**Output:** `{"greeting": "Hello, World!"}`

## Example: Array Processing

```typescript
export function handle(input: string): string {
  const data = JSON.parse(input);
  const numbers = data.numbers as i32[];
  
  let sum: i32 = 0;
  for (let i = 0; i < numbers.length; i++) {
    sum += numbers[i];
  }
  
  const avg = sum / numbers.length;
  
  return JSON.stringify({ 
    sum: sum, 
    average: avg,
    count: numbers.length 
  });
}
```

**Input:** `{"numbers": [1, 2, 3, 4, 5]}`  
**Output:** `{"sum": 15, "average": 3, "count": 5}`

## Example: Conditional Logic

```typescript
export function handle(input: string): string {
  const data = JSON.parse(input);
  const age = data.age as i32;
  
  let category: string;
  if (age < 13) {
    category = "child";
  } else if (age < 20) {
    category = "teenager";
  } else if (age < 65) {
    category = "adult";
  } else {
    category = "senior";
  }
  
  return JSON.stringify({ 
    age: age, 
    category: category 
  });
}
```

**Input:** `{"age": 25}`  
**Output:** `{"age": 25, "category": "adult"}`

## AssemblyScript Tips

### Type Annotations

AssemblyScript requires explicit types. Use these primitive types:

| Type | Description |
|------|-------------|
| `i32` | 32-bit signed integer |
| `i64` | 64-bit signed integer |
| `f32` | 32-bit float |
| `f64` | 64-bit float |
| `bool` | Boolean |
| `string` | String |

### Casting JSON Values

When parsing JSON, you must cast values to their types:

```typescript
const num = data.value as i32;      // integer
const str = data.name as string;    // string
const arr = data.items as i32[];    // array of integers
```

### String Concatenation

Use `+` for string concatenation:

```typescript
const result = "Hello, " + name + "!";
```

### Limitations

AssemblyScript has some limitations compared to TypeScript:
- No closures or arrow functions
- Limited standard library
- No dynamic property access (`obj[key]`)
- All variables must have explicit types

## Error Handling

If your function throws an error or crashes, the execution will be marked as `FAILED`:

```typescript
export function handle(input: string): string {
  const data = JSON.parse(input);
  const divisor = data.divisor as i32;
  
  // This will cause a runtime error if divisor is 0
  if (divisor == 0) {
    throw new Error("Division by zero");
  }
  
  const result = 100 / divisor;
  return JSON.stringify({ result: result });
}
```

When an error occurs:
- HTTP status is still `200 OK`
- Execution status is `FAILED`
- `errorMessage` contains the error details
- `output` is `null`

## Input Validation

Always validate your inputs:

```typescript
export function handle(input: string): string {
  const data = JSON.parse(input);
  
  // Check if required fields exist
  if (!data.has("value")) {
    return JSON.stringify({ error: "Missing 'value' field" });
  }
  
  const value = data.value as i32;
  
  // Validate ranges
  if (value < 0 || value > 100) {
    return JSON.stringify({ error: "Value must be between 0 and 100" });
  }
  
  // Process valid input
  return JSON.stringify({ doubled: value * 2 });
}
```

## Testing Locally

Before deploying, test your function source:

1. **Register the function:**
```bash
curl -X POST http://localhost:8080/functions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-function",
    "language": "assemblyscript",
    "source": "export function handle(input: string): string { ... }"
  }'
```

2. **Wait for compilation** (check status becomes `READY`)

3. **Test with various inputs:**
```bash
# Normal case
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"a": 1, "b": 2}}'

# Edge cases
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"a": 0, "b": 0}}'

# Error cases
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {}}'
```

## Best Practices

1. **Keep functions small and focused** - Do one thing well
2. **Validate inputs** - Check for required fields and valid ranges
3. **Return consistent output structure** - Same fields for success and error cases
4. **Use meaningful field names** - `{"sum": 8}` not `{"x": 8}`
5. **Handle edge cases** - Empty arrays, zero values, null fields
6. **Document your function** - Use the `description` field

## Complete Example: Calculator

Here's a more complete example that handles multiple operations:

```typescript
export function handle(input: string): string {
  const data = JSON.parse(input);
  
  // Validate required fields
  if (!data.has("operation") || !data.has("a") || !data.has("b")) {
    return JSON.stringify({ 
      success: false, 
      error: "Missing required fields: operation, a, b" 
    });
  }
  
  const operation = data.operation as string;
  const a = data.a as f64;
  const b = data.b as f64;
  
  let result: f64;
  
  if (operation == "add") {
    result = a + b;
  } else if (operation == "subtract") {
    result = a - b;
  } else if (operation == "multiply") {
    result = a * b;
  } else if (operation == "divide") {
    if (b == 0) {
      return JSON.stringify({ 
        success: false, 
        error: "Division by zero" 
      });
    }
    result = a / b;
  } else {
    return JSON.stringify({ 
      success: false, 
      error: "Unknown operation: " + operation 
    });
  }
  
  return JSON.stringify({ 
    success: true, 
    result: result 
  });
}
```

**Example usage:**
```bash
# Addition
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"operation": "add", "a": 10, "b": 5}}'
# Output: {"success": true, "result": 15}

# Division
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"operation": "divide", "a": 10, "b": 2}}'
# Output: {"success": true, "result": 5}

# Error case
curl -X POST http://localhost:8080/functions/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{"input": {"operation": "divide", "a": 10, "b": 0}}'
# Output: {"success": false, "error": "Division by zero"}
```

## Next Steps

- **[API Reference](../api.md)** - Complete endpoint documentation
- **[AssemblyScript Book](https://www.assemblyscript.org/introduction.html)** - Official AssemblyScript documentation
