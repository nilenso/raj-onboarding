# Roadmap

## Phase 0: Core FaaS - COMPLETE

**Status**: All issues closed, merged to main (December 27, 2025)

### User Stories Delivered

| ID | Story | Acceptance |
|----|-------|------------|
| US-1 | **Register a Function**: As a developer, I want to register a function by sending source code, so that the platform can compile it and make it executable. | API returns `functionId` immediately; function becomes `READY` or `FAILED` after compilation |
| US-2 | **Observe Compilation Outcome**: As a developer, I want to fetch function details, so that I can see whether compilation succeeded and view compile errors if it failed. | `GET /functions/{id}` shows `status` and `compileError` |
| US-3 | **Execute a Function**: As a developer, I want to execute a compiled function with JSON input, so that I can get JSON output. | Execution only allowed when function is `READY`; each execution persisted with status |
| US-4 | **Inspect Execution History**: As a developer, I want to see past executions of a function, so that I can debug and verify behavior. | `GET /functions/{id}/executions` lists executions; `GET /executions/{id}` shows details |
| US-O1 | **Debug Stuck Compilation** (Operational): As an operator, I want to see queue backlogs and correlate them to functions. | Queues inspectable in Postgres; logs include `functionId` |

### Endpoints Implemented

| Endpoint | Status |
|----------|--------|
| `POST /functions` | Done |
| `GET /functions` | Done |
| `GET /functions/{id}` | Done |
| `PUT /functions/{id}` | Done |
| `DELETE /functions/{id}` | Done |
| `POST /functions/{id}/execute` | Done |
| `GET /functions/{id}/executions` | Done |
| `GET /executions/{id}` | Done |

### Test Coverage

64 tests total: 6 common, 48 API, 10 compiler

---

## Phase 1: Auth + Permissions

**Goal**: Multi-user system with access control.

### New Entities

| Entity | Purpose |
|--------|---------|
| User | System user with role (Admin/Dev) |
| Group | Organizational unit for users |
| Session | Browser-based auth tokens |
| ApiKey | Programmatic access tokens |
| Environment | Key-value store for function config |

### Capabilities

- Admin: Full CRUD on all entities
- Dev: Execute functions, read environments
- Permission arrays on Function/Environment for access control
- Session-based auth (browser) + API key auth (programmatic)

### Access Control Logic

```
canAccess(user, resource):
  if user.role == Admin: return true
  if resource.allowed_users is empty AND resource.allowed_groups is empty:
    return true  # Public
  return user.id in resource.allowed_users 
      OR any(group in user.groups where group.id in resource.allowed_groups)
```

### Open Questions

- JWT vs session tokens?
- API key rotation and revocation policy?
- Integration with external identity providers?

---

## Phase 2: Analytics

**Goal**: Execution metrics and insights.

### Capabilities

- Execution duration tracking
- Success/failure rates
- Error type analysis
- Usage dashboards

### Open Questions

- Real-time vs batch processing?
- Same DB vs dedicated analytics store?
- Built-in dashboard vs external tool (Grafana)?

---

## Future Considerations

| Topic | Notes |
|-------|-------|
| Execution table growth | Archival strategy, partitioning, retention policies |
| Function versioning | Track versions, rollback capability |
| Additional languages | Rust, Go, C/C++ compilers |
| Async execution | Long-running functions with callbacks |
| WASM module caching | Cache parsed modules for performance |
| Rate limiting | Per-user/function execution limits |
| Cold start optimization | Pre-warm frequently used functions |
| Multi-region deployment | Geographic distribution |

### Draft Ideas

- Consider adding correlation IDs to execution responses for observability
- May want to add `durationMs` to execution responses
- Could add `GET /functions/{id}/stats` for execution statistics
