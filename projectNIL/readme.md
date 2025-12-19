# Project NIL

Towards a pedagogical CRUD to pick up on tooling and best practices.

## Documentation

| Document | Description |
|----------|-------------|
| [AGENTS.md](./AGENTS.md) | Agentic coding guidelines, branching strategy, commit conventions |
| [project-management.md](./project-management.md) | Issue types, Kanban workflow, GitHub CLI usage |
| [effective-java.md](./effective-java.md) | Effective Java best practices reference |
| [docs/domain.md](./docs/domain.md) | Domain model, entities, ERD, user stories |
| [docs/stack.md](./docs/stack.md) | Technology stack versions and rationale |
| [docs/toThink.md](./docs/toThink.md) | Open questions and future considerations |

### External Resources

- [GitHub Project Board](https://github.com/orgs/nilenso/projects/24/views/1) - Kanban tracking issues
- [Journal Branch](https://github.com/nilenso/raj-onboarding/tree/journal) - Daily documentation entries

### Workflows

| Workflow | Description |
|----------|-------------|
| **Project Management** | Issues tracked via GitHub Projects. See [project-management.md](./project-management.md) for CLI usage and conventions. |
| **Journalling** | Daily entries on `journal` branch, squash-merged to `main` with `[skip ci]`. |
| **Branching** | Feature work on `feature-issue-<N>` branches, technical tasks on `dev`. PRs required for all merges to `main` (except journal). |

---


# Appendix

## init configs

```
gradle init \
  --type java-application \
  --dsl groovy \
  --package rnil.enso \
  --project-name ProjectNIL  \
  --no-split-project  \
  --no-incubating  \
  --java-version 25
```
