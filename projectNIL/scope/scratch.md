# Scratch (Workbench)

Use this file as a scratchpad while converging on finalized scope docs.

## Notes
- Canonical docs live in this `projectNIL/scope/` directory.
- Keep statements here “tentative”; promote to other files only when stable.

## Open Questions
- What is the long-term direction for the compiler layer: JVM “compiler-engine” module vs per-language microservices (Node/Rust/etc.)?
- Should function execution be synchronous-only in Phase 0, or do we want async execution + polling?
- Do we want a single exported WASM ABI contract (e.g. `handle(input: string): string`) for all languages?

## Draft Decisions (Not Yet Final)
- Treat `projectNIL/scope/*` as the canonical spec; `projectNIL/docs/*` as historical/working notes.
