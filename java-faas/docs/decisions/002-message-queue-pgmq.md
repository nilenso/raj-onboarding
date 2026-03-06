# ADR 002: Message Queue - pgmq

**Status**: Accepted (December 2025)

## Context

Need async communication between API service and compiler services for non-blocking compilation.

## Decision

**pgmq** - PostgreSQL-native message queue.

- Queues: `compilation_jobs`, `compilation_results`
- Routing: App-level (filter by language in message)

## Options Considered

| Option | Verdict |
|--------|---------|
| RabbitMQ | Over-engineered for Phase 0 |
| Kafka | Overkill (no event streaming needs) |
| Redis queue | Another persistence layer |
| Simple PG table | Lacks queue optimizations |
| **pgmq** | Best fit |

## Rationale

**Why pgmq?**
- Single data store (PostgreSQL already in use)
- No additional infrastructure
- ACID guarantees
- SQL-based debugging
- Simple API

**Why not RabbitMQ?**
- Separate broker to manage
- Overkill for Phase 0 throughput
- More moving parts

## Consequences

**Positive**: Single DB to manage, simpler deployment, transactional consistency, easy debugging

**Negative**: Tight PostgreSQL coupling, lower throughput than dedicated brokers, polling-based

**Migration path**: Can switch to RabbitMQ if throughput needs increase.

## Setup

Using containerized PostgreSQL 18 with pgmq pre-installed:

```bash
podman run -d --name pgmq-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  ghcr.io/pgmq/pg18-pgmq:v1.8.0
```

## References

- [pgmq GitHub](https://github.com/pgmq/pgmq)
- [pgmq on PGXN](https://pgxn.org/dist/pgmq/)
- [Container images](https://github.com/pgmq/pgmq/pkgs/container/pg18-pgmq)
