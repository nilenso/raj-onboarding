# Raj's Onboarding

index/overview of progress/work done during Raj's Onboarding

## Overview


### phase 1: [java-faas](./java-faas)
 - refer internal [readme](./java-faas/readme.md) for setup and tutorial
 - refer architecture [presentation](./scratch/presentations/pres0/pres.org) for design overview
 - a basic FaaS
   - two microservices in Java 25 (orchestrator and compiler): (architecture)
   - transpilation target being wasm (via [chicory](https://github.com/dylibso/chicory))
   - persistence layer: postgres-18 and [pgmq-1.8](https://pgmq.github.io/pgmq/latest/)
   - schema migrations via [liquibase](https://www.liquibase.com/)
   
### phase 2: [clj-faas](./clj-faas)
 - refer internal [readme](./clj-faas/readme.md) for setup and tutorial
 - learning clojure : rewriting the orchestrator from the java-faas while retaining the java compiler
 
### phase 3: [goose-research](./goose-notes)
 - short lived : had to move on to picking up clojurescript and javascript ecosystem and concepts towards onboarding on CH

### phase 4: [cljs-scratch](./cljs-scratch)
 - quick ramp up : ending the onboarding soon

## Primary Literature

 - [x] Effective Java (phase 1)
 - [x] User Stories Applied (phase 2)
 - [ ] The Joy of Clojure : follow along exploratory source code [here](./scratch/prac/theJoyOfClojure/src) (phase 2,3,4)

## Onboarding Logs (sporadic/in-exhaustive)
 - [journals](./journal)
