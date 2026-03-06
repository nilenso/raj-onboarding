# Project Workflow

This document defines how work is organized, tracked, and delivered using GitHub Projects (Kanban).

## GitHub Setup

### Project Board

- **Project**: ProjectNIL
- **URL**: https://github.com/orgs/nilenso/projects/24
- **Repository**: nilenso/raj-onboarding

The board uses a custom "Stage" field for Kanban columns. Ensure you're grouping by "Stage" (not the default "Status" field).

### CLI Commands

```bash
# List all issues
gh issue list --repo nilenso/raj-onboarding

# Create an issue with labels
gh issue create --repo nilenso/raj-onboarding \
  --title "Issue title" \
  --body "Issue body" \
  --label "priority: medium"

# View project board
gh project view 24 --owner nilenso --web

# List project items
gh project item-list 24 --owner nilenso

# Create issue with type (via API)
gh api repos/nilenso/raj-onboarding/issues \
  --method POST \
  -f title="Issue title" \
  -f body="Issue body" \
  -f type="Feature" \
  -f "assignees[]=rajp152k"
```

## Philosophy: The Unit of Work

Our project management is grounded in the principle that **the unit of work is the fundamental abstraction**.

### Core Principles

1. **Slice vertically, not horizontally**: Each unit should deliver end-to-end value, not just "backend done"
2. **Keep context together**: All relevant information lives with the unit of work
3. **Done = Deployed**: A unit isn't complete until it's in production (or merged to main)
4. **Measure value, not activity**: Delivered value matters, not lines of code

### INVEST Criteria

Good units of work should be:

| Property | Meaning |
|----------|---------|
| **I**ndependent | Can be developed without blocking on others |
| **N**egotiable | Flexible scope, can be refined |
| **V**aluable | Delivers something the user cares about |
| **E**stimable | Can be sized for planning |
| **S**mall | Completable in hours to a few days |
| **T**estable | Has clear acceptance criteria |

Reference: [The Common Sense Unit of Work](https://blog.nilenso.com/blog/2025/09/17/the-common-sense-unit-of-work/)

## Issue Types

Use GitHub's native issue type mechanism:

| Type | Usage |
|------|-------|
| **Feature** | User-facing functionality (valuable slices) |
| **Task** | Enabler/engineering tasks that support features |
| **Bug** | Defects in existing functionality |

For spikes (time-boxed research), use a Task with "Spike:" prefix.

### Linking Technical Work

Technical tasks should never float independently. Always link to value:

```markdown
# Issue #42: Design database schema for authentication

**Enables:** #41 (User can register with email and password)
```

And in the parent:

```markdown
# Issue #41: User can register with email and password

**Depends on:** #42 (Database schema)
```

## Kanban Workflow

### Board Columns

| Column | Purpose | Entry Criteria |
|--------|---------|----------------|
| **Backlog** | Prioritized work waiting to be refined | Issue exists with basic description |
| **Ready** | Fully specified, can be picked up | Acceptance criteria defined, no blockers |
| **In Progress** | Currently being worked on | Work has started |
| **In Review** | PR open, awaiting review/CI | Code complete, PR submitted |
| **Done** | Merged and deployed | Merged to main, acceptance met |

### Labels

#### Status Labels

| Label | Description |
|-------|-------------|
| `status: blocked` | Waiting on external dependency or decision |
| `status: needs-refinement` | Requires more specification before Ready |

#### Priority Labels

| Label | Description |
|-------|-------------|
| `priority: high` | Do this first; blocking or critical |
| `priority: medium` | Important but not urgent |
| `priority: low` | Nice to have; when time permits |

## Issue Template

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

## Best Practices

### Writing Good Issues

1. **Start with "why"**: The summary should explain the value
2. **Verifiable acceptance criteria**: "User sees error message" not "Handle errors"
3. **Keep issues small**: If more than a few days, break it down
4. **Update as you learn**: Add context and decisions as you work

### Managing the Board

1. **Limit WIP**: 1-2 items in "In Progress" max for solo work
2. **Move issues promptly**: Update status as soon as it changes
3. **Review backlog weekly**: Re-prioritize based on learnings
4. **Close when done**: Don't let "Done" column grow indefinitely

### Connecting to Code

1. **Reference issues in commits**: `feat(auth): implement login flow (#41)`
2. **Reference issues in PRs**: Use "Closes #41" to auto-close on merge
3. **Link PRs to issues**: GitHub shows the connection automatically
