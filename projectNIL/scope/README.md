# ProjectNIL Scope (Canonical)

This directory contains the **finalized, canonical** end-to-end specification for ProjectNIL.

## What This Is
- A coherent, end-to-end view of the platform: entities, service boundaries, interfaces, DTOs, protocols, and operational practices.
- A “single source of truth” meant to be understandable without reading code.

## What This Is Not
- An implementation guide for *how* the code is structured today.
- A complete backlog/roadmap.

## Index
- `scope/architecture.md` — system boundaries and responsibilities
- `scope/contracts.md` — API + queue message contracts (DTOs) and invariants
- `scope/entities.md` — domain entities, state machines, and persistence shape
- `scope/flows.md` — end-to-end flows (success and failure), mermaid sequence diagrams
- `scope/practices.md` — retries, idempotency, observability, error handling, testing
- `scope/user-stories.md` — user stories that drive the flows

## Conventions
- “MUST/SHOULD/MAY” are used intentionally.
- Message payloads are shown as JSON; binary values are represented as base64 in JSON.
