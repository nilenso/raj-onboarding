# ADR 002: Message Queue - pgmq (PostgreSQL Queue)

## Status

**Accepted** (December 2025)

## Context

ProjectNIL requires asynchronous communication between the API service and compiler services. The function compilation process is long-running and should not block HTTP requests. A message queue enables decoupling these services.

**Requirements:**
- Reliable message delivery
- FIFO ordering (preserve compilation job order)
- Acknowledgment/retry capability
- Minimal operational overhead for Phase 0
- Single data store preference

## Options Considered

### Option 1: RabbitMQ

Standalone AMQP message broker with topic exchanges and flexible routing.

| Aspect | Assessment |
|--------|------------|
| Reliability | Excellent (persistent, HA available) |
| Routing | Flexible (topic exchanges, routing keys) |
| Operations | Requires separate broker, port (5672, 15672), container |
| Complexity | Medium (AMQP protocol, Spring AMQP integration) |
| Additional deps | Yes (separate service, separate persistence) |
| Scalability | Good (designed for scale) |
| Learning | Good (industry standard, widely known) |

**Verdict**: Over-engineered for Phase 0, adds operational complexity.

### Option 2: Apache Kafka

Distributed event streaming platform with topic-based architecture.

| Aspect | Assessment |
|--------|------------|
| Reliability | Excellent (distributed, replicated) |
| Routing | Topic-based (no exchange logic) |
| Operations | Heavy (cluster setup, Zookeeper/KRaft), requires tuning |
| Complexity | High (offset management, consumer groups) |
| Additional deps | Yes (separate service, significant resource footprint) |
| Scalability | Excellent (designed for scale) |
| Learning | Steep (complex concepts, operational burden) |

**Verdict**: Overkill for Phase 0 use case (not event sourcing, no high throughput needs).

### Option 3: Redis with Queue

Redis-based queue using LPUSH/RPOP or streams.

| Aspect | Assessment |
|--------|------------|
| Reliability | Good (persistence optional, AOF/RDB) |
| Routing | Limited (needs app-level routing) |
| Operations | Simple (single service) |
| Complexity | Low (straightforward API) |
| Additional deps | Yes (separate Redis service) |
| Scalability | Good for most cases (single instance) |
| Learning | Easy (simple data structures) |

**Verdict**: Good option, but introduces another persistence layer.

### Option 4: PostgreSQL JSONB Table (Simple Queue)

Use a PostgreSQL table as ad-hoc queue with polling.

| Aspect | Assessment |
|--------|------------|
| Reliability | Good (transactional, durable) |
| Routing | App-level (query filtering) |
| Operations | None (already have PostgreSQL) |
| Complexity | Low (SQL queries) |
| Additional deps | No (uses existing database) |
| Scalability | Adequate for Phase 0 |
| Learning | Easy (SQL) |

**Verdict**: Works but lacks dedicated queue optimizations (cleanup, indexing, message expiry).

### Option 5: pgmq (PostgreSQL Queue Extension)

Dedicated SQL-based message queue built on PostgreSQL with queue-specific optimizations.

| Aspect | Assessment |
|--------|------------|
| Reliability | Excellent (PostgreSQL durability, ACID) |
| Routing | App-level (message body routing) |
| Operations | None (PostgreSQL extension, single data store) |
| Complexity | Low (simple SQL API) |
| Additional deps | No additional services |
| Scalability | Good (PostgreSQL scalability) |
| Learning | Easy (SQL-like interface) |

**Verdict**: Best balance of simplicity, reliability, and operational fit.

## Decision

**pgmq (PostgreSQL message queue)** - Use pgmq as the message queue for async compilation.

### Specifics

1. **Implementation**: pgmq as PostgreSQL extension
2. **Queues**:
   - `compilation_jobs` - Compilation requests with language field
   - `compilation_results` - Compilation results
3. **Routing**: App-level (compiler services filter by language in message)
4. **Acknowledgment**: pgmq's archive feature for reliability
5. **Cleanup**: pgmq's automatic message cleanup

## Rationale

### Why pgmq?

1. **Single Data Store**: No additional infrastructure. PostgreSQL is already the primary database. Reduces deployment complexity and operational burden.

2. **ACID Guarantees**: Full transaction support. Message publish and function status update can be atomic, ensuring consistency.

3. **Simplicity**: SQL-based API. Developers familiar with SQL can understand and debug queue operations without learning AMQP or Kafka concepts.

4. **No Additional Services**: Eliminates the need to manage, monitor, and scale a separate message broker. Reduces the tech stack footprint.

5. **Suitable for Phase 0**: Compilation request volume is moderate. pgmq is more than adequate. Can migrate to RabbitMQ/Kafka if needed later.

6. **Durability**: Messages persisted in PostgreSQL. Loss-less queuing with transactional semantics.

7. **Developer Experience**: Easy debugging (query messages directly in DB), simple monitoring, clear error handling.

### Why not RabbitMQ?

- Adds operational complexity (separate broker, ports, monitoring)
- Overkill for Phase 0 requirements
- Requires Spring AMQP integration layer
- Separate persistence layer to manage
- Better suited for high-throughput, complex routing scenarios

### Why not Kafka?

- Distributed system with significant operational overhead
- Steep learning curve (offsets, consumer groups, rebalancing)
- Designed for event streaming (not needed for compilation requests)
- Resource-intensive for Phase 0 scale
- Requires cluster management

### Why not simple PostgreSQL table?

- pgmq provides optimized queue operations (archiving, cleanup, indexing)
- pgmq handles message lifecycle (visibility timeouts, poison pill detection)
- pgmq is purpose-built for queuing (better than generic table)

## Consequences

### Positive

- Single database to manage (no separate AMQP broker)
- Simpler deployment (fewer containers/services)
- SQL-based debugging (view messages directly)
- Full ACID transactionality
- Reduced operational overhead
- Easier to understand and maintain
- Aligns with "keep it simple for Phase 0" philosophy

### Negative

- Tight coupling to PostgreSQL (can't easily swap for different DB)
- Message throughput lower than dedicated brokers (acceptable for Phase 0)
- Polling-based consumers (slight latency vs push-based brokers)
- Less flexible routing (no AMQP-style exchanges)
- Limited to PostgreSQL ecosystem

### Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Compiler service throughput exceeds pgmq capacity | Monitor queue depth; pre-emptively optimize or migrate to RabbitMQ |
| Message visibility/processing guarantees | Use pgmq's archive/cleanup features; implement consumer deadletter handling |
| Debugging lost messages | Maintain detailed logs in message payload; use pgmq archive |
| PostgreSQL becomes bottleneck | Scale database separately; consider read replicas for monitoring |

## Rollout Plan

### Phase 0 (Now)
- Use pgmq for `compilation_jobs` and `compilation_results` queues
- Implement compiler services as pgmq consumers
- Monitor queue depth and message processing latency

### Future (if needed)
- If throughput requirements exceed pgmq, migrate to RabbitMQ
- Migration is possible due to message format abstraction in code
- Keep pgmq for other use cases (background jobs, notifications)

## References

- [pgmq GitHub](https://github.com/tembo-io/pgmq)
- [pgmq SQL API](https://tembo.io/docs/extensions/pgmq/)
- [PostgreSQL Extensions](https://www.postgresql.org/docs/current/external-extensions.html)
- ADR 001: WASM Runtime (mentions message queue for compiler reliability)
- Phase 0 Docs: `docs/phase0.md`
- Technology Stack: `docs/stack.md`

## Related

- GitHub Discussion: Message Queue Technology Choice
- Architecture: `docs/stack.md` (service breakdown)
- Phase 0: `docs/phase0.md` (message format specifications)
