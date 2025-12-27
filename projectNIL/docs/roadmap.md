# Roadmap

## Phase 0: Core FaaS - COMPLETE

**Status**: All issues closed, merged to main (December 27, 2025)

**Implemented**:
- Register, list, get, update, delete functions
- Compile source to WASM (AssemblyScript)
- Execute functions with JSON input/output
- Track execution history
- PGMQ-based async compilation pipeline
- Canonical DTO serialization

**Endpoints**:
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

**Test Coverage**: 64 tests (6 common, 48 API, 10 compiler)

---

## Phase 1: Auth + Permissions

**Goal**: Multi-user system with access control.

**New Entities**:

| Entity | Purpose |
|--------|---------|
| User | System user with role (Admin/Dev) |
| Group | Organizational unit for users |
| Session | Browser-based auth tokens |
| ApiKey | Programmatic access tokens |
| Environment | Key-value store for function config |

**Capabilities**:
- Admin: Full CRUD on all entities
- Dev: Execute functions, read environments
- Permission arrays on Function/Environment for access control
- Session-based auth (browser) + API key auth (programmatic)

**Access Control Logic**:
```
canAccess(user, resource):
  if user.role == Admin: return true
  if resource.allowed_users is empty AND resource.allowed_groups is empty:
    return true  # Public
  return user.id in resource.allowed_users 
      OR any(group in user.groups where group.id in resource.allowed_groups)
```

**Open Questions**:
- JWT vs session tokens?
- API key rotation policy?
- Integration with external identity providers?

---

## Phase 2: Analytics

**Goal**: Execution metrics and insights.

**Capabilities**:
- Execution duration tracking
- Success/failure rates
- Error type analysis
- Usage dashboards

**Open Questions**:
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
