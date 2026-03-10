# Design mulls

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
        - buffered work chan helps out with queue backpressure  (sizing at 2*batch-size)
        - https://www.tedinski.com/2019/03/05/backpressure.html

 - visibility timeout for result reads during polls
    - setting a viz timeout for messages to 0 also works when I pick up work cause there's just one poller and am removing the messages from the queue in case of succesful application 
    - but would not like these to be re-read on the next polling cycle in case the transactions from the past state still haven't been completed 
    - need to be empirical with this number : will iterate and updates
    
 - use native :read-count of pgmq messages for a retry upper bound : in the compilation results handler

### Schema management 

 - need to figure out where the common source of truth needs to be placed : as of now is just tribal knowledge : in the java codebase was a separate

 - compilations-jobs
```
package com.projectnil.common.domain.queue;
import java.util.UUID;
public record CompilationJob(
    UUID functionId,
    String language,
    String source
) {}
```
 - compilation-results
```
package com.projectnil.common.domain.queue;
import java.util.UUID;
public record CompilationResult(
    UUID functionId,
    boolean success,
    byte[] wasmBinary,
    String error
) {}
```

### Chicory Wasm
 - this was going to be a bijection from java to clojure (transliteration into s-exps) : instead using this as an opportunity to understand the build mechanisms of shipping java source in clojure project 
 - just copying the java wasm source in here and exposing a wrapper
 - MultiLanguage project (Intra and Inter Service)
 - quite a bit of reading : https://clojure.org/guides/tools_build
 - https://github.com/clojure/tools.build
 - flat file struct works for java source : compilation yields the hierarchy (for class files) declared in the nomenclature
 - import the abstractions in wasm.clj and expose internals neatly without leaking out the Java'ish nature of things 



# Defers

 - [ ] schema source of truths for multilanguage projects
  - protobufs?

 - [ ] retry mechanism with a pipeline for the functions post handler
  - current core.async thread for post function handler should be sync : timeout if unable to push to pipeline queue : more accurate info regarding state of submitted func : similar pattern to what the poller is doing (communicating sequential processes)
