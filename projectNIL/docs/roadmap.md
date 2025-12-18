# Roadmap

## Phase 0: Core FaaS (Current)

**Goal**: Minimal viable FaaS without auth.

- Register, update, delete functions
- Compile source to WASM (AssemblyScript)
- Execute functions with input/output
- Track execution history

**Entities**: Function, Execution

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
- Built-in dashboard vs external tool?

---

## Future Considerations

| Topic | Notes |
|-------|-------|
| Execution table growth | Archival strategy, partitioning, retention policies |
| Function versioning | Track versions, rollback capability |
| Additional languages | Rust, Go, C/C++ compilers |
| Async execution | Long-running functions with callbacks |
