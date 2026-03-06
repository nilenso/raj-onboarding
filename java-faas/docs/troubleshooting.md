# Troubleshooting

Common issues and their solutions when working with ProjectNIL.

## Quick Diagnostics

```bash
# Check service health
curl http://localhost:8080/health
curl http://localhost:8081/health

# View logs
podman compose logs -f api
podman compose logs -f compiler
podman compose logs -f postgres

# Check container status
podman ps -a
```

## Common Issues

### Function Stuck in PENDING Status

**Symptoms**: Function never transitions to COMPILING or READY.

**Diagnosis**:
```bash
# Check if compiler is running
podman ps | grep compiler

# Check compiler logs
podman compose logs compiler

# Verify pgmq queues exist
podman exec projectnil-db psql -U projectnil -d projectnil \
  -c "SELECT pgmq.list_queues();"
```

**Solutions**:
1. Start the compiler service: `podman compose --profile full up -d compiler`
2. If queues missing, run migrations: `podman compose --profile migrate up liquibase`
3. Check compiler logs for connection errors

### Compilation Fails

**Symptoms**: Function status is FAILED with compileError.

**Diagnosis**:
```bash
# Check the compile error
curl http://localhost:8080/functions/{id} | jq '.compileError'
```

**Common causes**:
- Syntax errors in AssemblyScript source
- Missing `export function handle(input: string): string` signature
- Missing `--exportRuntime` flag (handled automatically by compiler)

**Solutions**:
1. Fix the source code
2. Update the function: `PUT /functions/{id}` with corrected source
3. Function will transition back to PENDING for recompilation

### Execution Fails

**Symptoms**: Execution status is FAILED with errorMessage.

**Diagnosis**:
```bash
curl http://localhost:8080/executions/{id} | jq '.errorMessage'
```

**Common causes**:
- Runtime trap (unreachable instruction, division by zero)
- Execution timeout (default: 10 seconds)
- Invalid JSON output from function

**Note**: User code failures return HTTP 200 with `status: FAILED`. This is intentional - the platform operated correctly; the failure is in user code.

### Database Connection Issues

**Symptoms**: 500 errors, "Connection refused" in logs.

**Diagnosis**:
```bash
# Check if postgres is healthy
podman exec projectnil-db pg_isready -U projectnil

# Check database logs
podman compose logs postgres
```

**Solutions**:
1. Ensure postgres is running: `podman compose up -d postgres`
2. Wait for postgres to be healthy before starting other services
3. Check `SPRING_DATASOURCE_URL` environment variable

### Invalid UUID Returns 500

**Symptoms**: Requests with invalid UUID path parameters return 500 instead of 400.

**Example**:
```bash
curl http://localhost:8080/functions/not-a-uuid
# Returns 500 instead of 400
```

**Status**: Known issue. The GlobalExceptionHandler needs specific handlers for `MethodArgumentTypeMismatchException`.

### HTTP Errors Return 500

**Symptoms**: 404 and 405 errors incorrectly return 500.

**Example**:
```bash
curl http://localhost:8080/nonexistent  # Should be 404, returns 500
curl -X PATCH http://localhost:8080/functions  # Should be 405, returns 500
```

**Status**: Known issue. Requires exception handlers for:
- `NoResourceFoundException` → 404
- `HttpRequestMethodNotSupportedException` → 405

## Production Troubleshooting

### SSH Access

```bash
ssh root@157.245.108.179
```

### Check Service Status

```bash
podman ps -a
podman logs projectnil-api --tail 100
podman logs projectnil-compiler --tail 100
podman logs projectnil-db --tail 100
```

### Database Inspection

```bash
# Connect to database
podman exec -it projectnil-db psql -U projectnil -d projectnil

# Check tables
\dt

# Check pgmq extension
\dx

# List queues
SELECT pgmq.list_queues();

# View pending jobs
SELECT * FROM pgmq.read('compilation_jobs', 30, 10);

# View pending results
SELECT * FROM pgmq.read('compilation_results', 30, 10);
```

### Migration Status

```bash
# Check migration history
podman exec projectnil-db psql -U projectnil -d projectnil \
  -c "SELECT * FROM databasechangelog;"
```

### Reset Database (DATA LOSS)

```bash
# Stop all services
podman compose --profile full down

# Remove volume
podman volume rm infra_postgres_data

# Start fresh
podman compose up -d postgres
podman compose --profile migrate up liquibase
```

## Common Errors Reference

| Error | Likely Cause | Solution |
|-------|--------------|----------|
| 500 on POST /functions | PGMQ not initialized | Run migrations |
| Function stuck PENDING | Compiler not running | Start compiler service |
| 415 Unsupported language | Wrong language value | Use "assemblyscript" |
| 400 Input must be object | Primitive/array input | Pass JSON object |
| WASM trap | User code error | Fix function source |
| Execution timeout | Infinite loop | Fix function source |

## Getting Help

1. Check the [API Reference](api.md) for correct request formats
2. Review [Writing Functions](guides/writing-functions.md) for AssemblyScript patterns
3. Check [Infrastructure](infrastructure.md) for deployment details
