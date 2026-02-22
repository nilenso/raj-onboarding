(ns api.handlers.compilations
  (:require
   [api.db :as db]
   [next.jdbc :as jdbc]
   [api.pgmq :as pgmq]
   [next.jdbc.types :as jdbc-types :refer [as-binary]]
   [api.utils :as u :refer [throw-error! uuidfy]]
   [taoensso.telemere :as t :refer [log!]]))

;; handler for compilation results received from pgmq,
;; which are then stored in the database and used to update the status of the compilation job
;; these aren't http handlers, but are called by the pgmq polling loop

(defn process-compilation-result
  "update a function status (ready, failed) from compiling (idempotent : ignore if already applied)"
  [compilation-result]
  (let [fn-id (uuidfy (:id compilation-result))
        current (db/get-function-by-id fn-id)
        applied? (get #{"READY" "FAILED"} (:functions/status current))]
    ;; when already in a terminal state, ignore the update and log a warning
    ;; otherwise, apply the update to the fn db record
    (if applied?
      (log! {:level :warn
             :id ::compilation-update-tried-on-terminal-state
             :data {:fn-id fn-id
                    :current-status (:functions/status current)
                    :incoming-result compilation-result}})
      (try
        (db/update-function fn-id (dissoc compilation-result
                                          :id
                                          :language
                                          :source
                                          :msg-id))
        (log! {:level :debug
               :id ::compilation-update-applied
               :data {:fn-id fn-id
                      :new-fn-map compilation-result}})
        (catch Exception e
          (throw-error! ::compilation-update-failed e
                        {:fn-id fn-id
                         :compilation-result compilation-result}))))
    ;; delete the pgmq message after processing, to prevent re-processing on the next poll
    ;; not using a transaction here, cause of the way the db api is structured, but if the delete fails, the worst that happens is we try to re-apply the same update on the next poll, which is idempotent
    (try
      (pgmq/delete-pgmq-result (:msg-id compilation-result))
      (log! {:level :debug
             :id ::compilation-result-pgmq-message-deleted
             :data {:fn-id fn-id
                    :msg-id (:msg-id compilation-result)}})
      (catch Exception e
        (throw-error! ::compilation-result-pgmq-message-delete-failed e)))))

(comment
  (db/start-pool!)
  (pgmq/purge-pgmq-queues)

  ;; simuating a registration

  ;; add-fn
  
  (def test-fn
    (db/add-function {:name "test-fn"
                      :language "clojure"
                      :source "(println \"Hello, World!\")"
                      :status "pending"}))

  (def test-fn-id (:functions/id test-fn))

  (keys (db/get-function-by-id test-fn-id))

  ;; goes to compilation_jobs, picked up by compiler, and then compiler publishes to compilation_results, which is read by the pgmq polling loop and processed by the handler above

  ;; simulating a compilation result push from compiler services
  (pgmq/publish-pgmq-message "compilation_results"
                             {:functions/id test-fn-id
                              :functions/language "clojure"
                              :functions/source "(println \"Hello, World!\""
                              :functions/status "success"
                              :functions/compile_error ""
                              :functions/wasm_binary (as-binary "00")})

  ;; the poller will read like this
  
  (def test-comp-result
    (pgmq/read-pgmq-result))

  test-comp-result

  ;; and then process like this
  (process-compilation-result test-comp-result)

  (pgmq/delete-pgmq-result (:msg-id test-comp-result))
  

  )
