# Open Questions & Future Considerations

This document tracks design decisions that need further research or discussion.

## Function Body Storage (Spike Required)

**Issue:** How do we store and execute function bodies?

**Options to research:**

| Option | Pros | Cons |
|--------|------|------|
| **Code string** (interpreted at runtime) | Simple to store | Security risk (code injection), language-specific |
| **Container image reference** | Secure, language-agnostic | Complex infrastructure, slower cold starts |
| **WASM modules** | Sandboxed, portable, modern | Learning curve, toolchain maturity |
| **External reference** (S3/storage URL) | Flexible, decoupled storage | Extra hop, availability dependency |
| **Stored procedures** (PostgreSQL) | Fast, transactional | Database coupling, limited language support |

**Questions to answer:**
- What languages should functions support?
- What's the execution model? (sync vs async)
- What sandboxing/isolation is needed?
- What's the acceptable cold start latency?

**Related issue:** See spike issue for research task.

---

## Execution Table Growth

**Concern:** Executions will accumulate rapidly over time.

**Considerations:**
- Archival strategy (move old executions to cold storage)
- Table partitioning by time
- Retention policies (auto-delete after N days?)

**Phase:** Address in Phase 2 (Analytics) when we have real usage patterns.

---

## Function Versioning

**Concern:** Currently, updating a function overwrites it.

**Questions:**
- Should we track function versions?
- What happens to historical executions if function logic changes?
- Do we need rollback capability?

**Phase:** Revisit in Phase 1.

---

## Permission Array Performance

**Concern:** Storing `UUID[]` for `allowed_users` and `allowed_groups` on Function/Environment.

**Trade-offs:**
- PostgreSQL supports arrays natively (good)
- Querying "all functions user X can access" requires `ANY()` queries
- May be slower at scale compared to join table

**Decision:** Acceptable for pedagogical scope. Revisit if scaling becomes a concern.

---

## Analytics Layer (Phase 2)

**To define:**
- What metrics to track? (execution duration, success rate, error types)
- Real-time vs batch processing?
- Storage strategy (same DB vs dedicated analytics store)
- Visualization approach (built-in dashboard vs external tool)

**Phase:** Defer to Phase 2.
