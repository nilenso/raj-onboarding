# Scratch (Workbench)

Use this file as a scratchpad while converging on finalized scope docs.

## Notes
- Canonical docs live in this `projectNIL/scope/` directory.
- Keep statements here "tentative"; promote to other files only when stable.

## Resolved Questions (Phase 0)

These questions from earlier planning have been resolved:

| Question | Resolution |
|----------|------------|
| Compiler direction: JVM module vs per-language microservices? | **Per-language microservices**. Current: Node.js-based AssemblyScript compiler. Future languages will have separate services. |
| Sync vs async execution in Phase 0? | **Synchronous only**. Execution blocks until WASM returns. Async execution is a Phase 2+ consideration. |
| Single WASM ABI contract for all languages? | **Yes**. All functions export `handle(input: string): string`. See `scope/contracts.md` Section 4. |

## Open Questions (Phase 1+)

- Authentication: JWT vs session tokens?
- API key rotation and revocation policy?
- Function versioning: how to track and rollback?
- Execution table partitioning/archival strategy?
- Rate limiting: per-user, per-function, or both?

## Draft Ideas (Not Yet Final)

- Consider adding correlation IDs to execution responses for observability
- May want to add `durationMs` to execution responses
- Could add `GET /functions/{id}/stats` for execution statistics
