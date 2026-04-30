# Raj's Onboarding

index/overview of progress/work done during Raj's Onboarding

## Overview

### phase 1: [java-faas](./java-faas) 
#### late Nov 25 to early Jan 26
 - refer internal [readme](./java-faas/readme.md) for setup and tutorial
 - refer architecture [presentation](./scratch/presentations/pres0/pres.org) for design overview
 - a basic FaaS
   - two microservices in Java 25 (orchestrator and compiler): (architecture)
   - transpilation target being wasm (via [chicory](https://github.com/dylibso/chicory))
   - persistence layer: postgres-18 and [pgmq-1.8](https://pgmq.github.io/pgmq/latest/)
   - schema migrations via [liquibase](https://www.liquibase.com/)
   
### phase 2: [clj-faas](./clj-faas)
#### early Jan 26 to Mid March 26
 - refer internal [readme](./clj-faas/readme.md) for setup and tutorial
 - learning clojure : rewriting the orchestrator from the java-faas while retaining the java compiler
 
### phase 3: [goose-research](./goose-notes)
#### Mid March 26 to late March 26
 - short lived : had to move on to picking up clojurescript and javascript ecosystem and concepts towards onboarding on CH

### phase (not 3): [cljs-scratch](./cljs-scratch)
#### ~two weeks
 - quick ramp up : ending the onboarding soon
 - short-lived , back to goose
 
### phase 3: [Integration Test DSL for Goose]
#### early Arpil 26 to late April 26
 - integration test harness for goose : https://github.com/nilenso/goose/pull/214

## Primary Literature

 - [x] Effective Java (phase 1)
 - [x] User Stories Applied (phase 2)
 - [x] The Joy of Clojure : follow along exploratory source code [here](./scratch/prac/theJoyOfClojure/src) (phase 2,3)

## Onboarding Logs (sporadic/in-exhaustive)
 - [journals](./journal)
