# Project NIL

 - towards a pedagogical CRUD to pick up on tooling and best practices

## Ideation Seed

 - shared calculator microservice cluster
 - CRUD on calculation jobs (logged in DB)
 - opportunity to introduce probabilistic delays and test out benchmarking and profiling ecosystem as well
 
## Scope & Phases (documentation in progress)

 - project management being done [here](progress.org)

### Arch

```mermaid
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
