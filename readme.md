# Raj's Onboarding

index/overview of progress/work done during Raj's Onboarding

## Overview

 
### [java-faas](./java-faas)
 - refer internal [readme](./java-faas/readme.md) for setup and tutorial
 - refer architecture [presentation](./scratch/presentations/pres0/pres.org) for design overview
 - a basic FaaS
   - two microservices in Java 25 (orchestrator and compiler): (archictecture)
   - transpilation target being wasm (via [chicory](https://github.com/dylibso/chicory))
   - persistence layer: postgres-18 and [pgmq-1.8](https://pgmq.github.io/pgmq/latest/)
   - migrations via [liquibase](https://www.liquibase.com/)
   
### clj-faas
 - refer internal [readme](./clj-faas/readme.md) for setup and tutorial
 - learning clojure : rewriting the orchestrator from the java-faas while retaining the java compiler

### Books

 - [x] Effective Java
 - [x] User stories applied
 - [ ] The joy of clojure

### onboarding logs
 - [journals](./journal)
