# ProjectNIL (but in clojure)

## Overview
 - writing the overall orchestrator in clojure, while keeping the older java compiler service


# Design Mulls

### Meta

 - cider, dap, emacs
 - no LLMs for writing code: occasional tab complete from copilot for some unit tests
    - only employed pedagogically for discussing arch, clojure internals and mulling over design
 - repl driven : embedded nrepl into the api server to inspect and iterate upon dev'able state
 - TDD when it felt natural
    - else, comment blocks represent the repl interaction

### structured logging with qualified keywords
 - all maps : convenient parsing for observability in hindsight

### state management with atoms (configs, pools)
 - hybrid api for db and q (state and stateless for datasource)
 - to make the db and q api polymorphic with a default pool as well as allow for transaction specific data sources
 - transactions when need to, else quick access through the existing pool

### -main: system lifecycle orchestrator
 - read configs
 - start servers, register hooks

### simulating transactions with compilation handler

 -  https://github.com/seancorfield/next-jdbc/blob/develop/doc/transactions.md
 - current top level abstractions (see db.clj and pgmq.clj) don't take in conn-pool and use a stateful one
    - given pg rdbms and pg mq use the same conn (and it's masked), have just used the stateful one so far : could consider using a dedicated api for transactions
    - need to update the api for it to be polymorphic : use the default conn pool when db level atomics (reads) vs take in a transaction dedicated conn when needed

### Idempotent compilations handler

### polling compilation results

 - simple short poller via core.async
 - alts! to wait on a stop-chan and a poll-interval-ms config'd timeout chan
 - stop-chan exposed via return and registerd as shutdown hook as the gateway
 - concurrency contenders
    - for a given amount of reads, mapv (force eager eval) the futures of results into compilation results handler  
    - fixed worker pipeline from core.async : decided to proceed with this given am using core.async anyway
