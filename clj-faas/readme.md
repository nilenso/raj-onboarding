# ProjectNIL (but in clojure)

## Overview
 - writing the overall orchestrator in clojure, while keeping the older java compiler service

# Design thoughts
 - see [design.md](./design.md)

## Deps setup

 - setup java deps first for running the compiler: 
   - refer [../java-faas/readme.md](../java-faas/readme.md)
    - need openjdk25, node.js and asc compiler for assemblyscript 
- install [clj (v1.12+)](https://clojure.org/guides/install_clojure) and [babashka(v1.12+)](https://github.com/babashka/babashka#installation)

## bb tasks
   
```
The following tasks are available:

run-api-dev       run the api server[DEV]
run-api-prod      run the api server[PROD]
compile-java      compile java sources
test              Run tests using the JVM clj tool
dev-deps-up       pg dev up + migrations
dev-deps-down     pg dev container down
dev-deps-logs     pg dev container logs
test-deps-up      pg test up + migrations
test-deps-down    pg test container down
test-deps-logs    pg test container logs
lint              clj-kondo linting
```

## Running the project

 - checkout [architecture](../scratch/presentations/pres0/arch.png) for broader context
 
 - setup dependencies (postgres + pgmq):
    - `bb dev-deps-up`
    - this downloads postgres+pgmq images and runs migrations
    - verify when healthy `podman ps`

 - run the compiler from (`cd ../java-faas && make compiler`)
    - init configs for that stored [here](../java-faas/services/compiler/src/main/resources/application.yaml)
    
 - run the orchestrator server
    - init configs for that stored [here](./resources/)
    - `bb run-api-prod` (for logging level :info)
    - `bb run-api-dev` (for logging level :debug)
    - this starts an embedded nrepl server, an http server, and a poller for picking up compilation results from the queue

