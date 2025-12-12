# Project NIL

Towards a pedagogical CRUD to pick up on tooling and best practices.

## Documentation

| Document | Description |
|----------|-------------|
| [AGENTS.md](./AGENTS.md) | Agentic coding guidelines, branching strategy, commit conventions |
| [project-management.md](./project-management.md) | Issue types, Kanban workflow, GitHub CLI usage |
| [effective-java.md](./effective-java.md) | Effective Java best practices reference |
| [domain.org](./domain.org) | Domain modelling notes |
| [progress.org](./progress.org) | Project progress tracking |

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

## Ideation (reverse chronological log)

### Domain Modelling

 - modeling in [domain.org](./domain.org)

### Function as a Service

 - Function as a Service : see journals : [3rd-Dec-2025](../journal/3-Dec-2025.org)

###  Over-engineered Calculator

 - shared calculator microservice cluster 
 - CRUD on calculation jobs (logged in DB)
 - opportunity to introduce probabilistic delays and test out benchmarking and profiling ecosystem as well
 
``` mermaid
graph TD
    A[User] --> B[Orchestrator Service]

    B --> C[Operations Services]
    C --> C1[Addition]
    C --> C2[Subtraction]
    C --> C3[Multiplication]
    C --> C4[Division]

    B --> D[Persistence Service]
    D --> E[PostgreSQL Database]

    subgraph Application
        B
        C
        D
        C1
        C2
        C3
        C4
    end
```
 
## Scope & Phases (documentation in progress)

 - project management being done [here](progress.org)

### Arch


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

## TechStack

 - Microservices : spring boot
 - Testing : JUnit, JBehave
 - Scope Documentation : Gherkin (Cucumber) 
 - Shared Calculation Log : Postgres 
