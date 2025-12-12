# Project Management Guidelines

This document defines how work is organized, tracked, and delivered in ProjectNIL using GitHub Projects (Kanban).

## Philosophy: The Unit of Work

Our project management is grounded in the principle that **the unit of work is the fundamental abstraction** in software development. Getting this right determines the effectiveness of everything built on top — planning, tracking, and coordination.

### Core Principles

1. **Slice vertically, not horizontally** — Each unit should deliver end-to-end value where possible, not just "backend done" or "database schema complete."

2. **Keep context together** — All relevant information (specs, conversations, decisions, unknowns) should live with the unit of work.

3. **Done = Deployed** — A unit isn't complete until it's in production (or merged to main, for a solo project).

4. **Measure value, not activity** — Lines of code and commit frequency aren't productivity; delivered customer value is.

### The INVEST Criteria

Good units of work should be:

| Property | Meaning |
|----------|---------|
| **I**ndependent | Can be developed without blocking on others |
| **N**egotiable | Flexible scope, can be refined as understanding grows |
| **V**aluable | Delivers something the customer/user cares about |
| **E**stimable | Can be sized for planning purposes |
| **S**mall | Completable in a reasonable time (hours to a few days) |
| **T**estable | Has clear acceptance criteria |

Reference: [The Common Sense Unit of Work](https://blog.nilenso.com/blog/2025/09/17/the-common-sense-unit-of-work/)

---

## Issue Types

Not all work delivers direct customer value, and that's okay. Use labels to distinguish work types:

| Label | Purpose | Example |
|-------|---------|---------|
| `type: feature` | Valuable slices — user-facing functionality | "User can register with email" |
| `type: technical` | Enabler/engineering tasks that support features | "Design database schema for auth" |
| `type: spike` | Time-boxed research/investigation | "Spike: Evaluate JWT vs session auth" |
| `type: bug` | Defects in existing functionality | "Login fails with special characters" |
| `type: chore` | Tech debt, maintenance, dependencies | "Update Gradle to 8.x" |

### Linking Technical Work to Value

Technical tasks should not float independently. Always ask:
- **Why** are we doing this?
- **What customer value** does it enable?
- Should we **defer** it until we actually need it?

Use GitHub issue references to link technical work to the feature it enables:

```markdown
# Issue #42: Design database schema for authentication

**Enables:** #41 (User can register with email and password)
```

And in the parent feature issue:
```markdown
# Issue #41: User can register with email and password

**Depends on:** #42 (Database schema)
```

---

## Kanban Workflow

### Board Columns

| Column | Purpose | Entry Criteria |
|--------|---------|----------------|
| **Backlog** | Prioritized work waiting to be refined | Issue exists with basic description |
| **Ready** | Fully specified, can be picked up immediately | Acceptance criteria defined, no blockers |
| **In Progress** | Currently being worked on | Work has started |
| **In Review** | PR open, awaiting review/CI | Code complete, PR submitted |
| **Done** | Merged and deployed | Merged to main, acceptance criteria met |

### Workflow Labels

Use these labels to add context to issue status:

| Label | Meaning |
|-------|---------|
| `status: blocked` | Waiting on external dependency or decision |
| `status: needs-refinement` | Requires more specification before Ready |

### Priority Labels

| Label | Meaning |
|-------|---------|
| `priority: high` | Do this first; blocking other work or critical |
| `priority: medium` | Important but not urgent |
| `priority: low` | Nice to have; do when time permits |

---

## Issue Template

When creating issues, include:

```markdown
## Summary
Brief description of what this issue accomplishes.

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Context
Why this is needed. Link to related issues/discussions.

## Technical Notes (optional)
Implementation hints, constraints, or decisions.

## Links
- **Enables:** #XX (if this is a technical task)
- **Depends on:** #XX (if blocked by another issue)
```

---

## Best Practices

### Writing Good Issues

1. **Start with "why"** — The summary should explain the value, not just the task.
2. **Make acceptance criteria verifiable** — "User sees error message" not "Handle errors."
3. **Keep issues small** — If it takes more than a few days, break it down.
4. **Update as you learn** — Add context, decisions, and blockers to the issue as you work.

### Managing the Board

1. **Limit work in progress** — For solo work, 1-2 items in "In Progress" max.
2. **Move issues promptly** — Update status as soon as it changes.
3. **Review backlog weekly** — Re-prioritize based on what you've learned.
4. **Close issues when done** — Don't let "Done" column grow indefinitely.

### Connecting to Code

1. **Reference issues in commits** — `feat(auth): implement login flow (#41)`
2. **Reference issues in PRs** — Use "Closes #41" to auto-close on merge.
3. **Link PRs to issues** — GitHub will show the connection automatically.
