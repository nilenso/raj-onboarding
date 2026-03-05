(ns api.pgmq
  (:require
   [next.jdbc :as jdbc :refer [execute! execute-one!]]
   [clojure.set :as set :refer [subset?]]
   [taoensso.telemere :as t :refer [log!]]
   [api.db :as db :refer [get-pool]]
   [clojure.walk :refer [keywordize-keys]]
   [api.utils :as u :refer [throw-error! <-pgobject ->pgobject]]))

(defn purge-pgmq-queues []
  (try
    (let [results (mapv #(execute! (get-pool) ["SELECT pgmq.purge_queue(?)" %]) ["compilation_jobs" "compilation_results"])]
      (log! {:level :debug
             :msg "Purged pgmq queues"})
      results)
    (catch Exception e
      (throw-error! ::pgmq-queue-purges-failed e))))

(defn publish-pgmq-message
  "publish a message to pgmq, given the queue and function map"
  [queue fn-map]
  (try
    (let [result (execute-one! (get-pool) ["SELECT pgmq.send(?, ?)" queue (->pgobject fn-map)])]
      (log! {:level :debug
             :msg "Published job to pgmq"
             :data {:queue queue
                    :fn-map fn-map
                    :result result}})
      result)
    (catch Exception e
      (throw-error! ::pgmq-publish-failed e))))

(defn- build-compilation-job
  "extracts a compiler compliant function map from a db function record, to be sent as a compilation job to pgmq"
  [fn-map]
  {:functionId (:functions/id fn-map)
   :language (:functions/language fn-map)
   :source (:functions/source fn-map)})

(defn publish-pgmq-job
  "publish a job to pgmq, given a function map"
  [fn-map]
  (publish-pgmq-message "compilation_jobs" (build-compilation-job fn-map)))

(defn read-from-pgmq
  "read qty messages from queue with visibility timeout of vt, default being reading one without marking it as invisible"
  ([queue]
   (read-from-pgmq queue 1 0))
  ([queue qty vt]
   (try
     (let [results (execute! (get-pool) ["SELECT * FROM pgmq.read(?, ?::integer, ?::integer, ?::jsonb)" queue vt qty (->pgobject {})])]
       (log! {:level :debug
              :msg "Read from pgmq"
              :data {:queue queue
                     :results results}})
       (mapv #(assoc
               (keywordize-keys (<-pgobject (:message %)))
               :msg-id (:msg_id %))
             results))
     (catch Exception e
       (throw-error! ::pgmq-read-failed e {:queue queue})))))

(defn- valid-compilation-result?
  "validate that the compilation result contains the required keys"
  [compilation-result]
  (subset? #{:functionId :success :wasmBinary :error} (set (keys compilation-result))))

(defn read-pgmq-result
  "read and validate compilation result from pgmq"
  []
  (let [comp-result (first (read-from-pgmq "compilation_results"))]
    (if (or (nil? comp-result) ;; allow nil result for empty queue case
            (valid-compilation-result? comp-result))
      comp-result
      (throw-error! ::pgmq-result-missing-keys nil {:comp-result comp-result}))))

(defn read-pgmq-results-batch
  [batch-size vt]
  (let [results (read-from-pgmq "compilation_results" batch-size vt)]
    (if (every? valid-compilation-result? results)
      results
      (throw-error! ::pgmq-result-missing-keys nil {:comp-results results}))))

(defn delete-pgmq-msg
  "delete a message from pgmq given the queue and message id"
  [queue msg-id]
  (try
    (let [result (execute-one! (get-pool) ["SELECT pgmq.delete(?, ?)" queue msg-id])]
      (log! {:level :debug
             :msg "Deleted message from pgmq"
             :data {:queue queue
                    :msg-id msg-id
                    :result result}})
      result)
    (catch Exception e
      (throw-error! ::pgmq-delete-failed e {:queue queue :msg-id msg-id}))))

(defn delete-pgmq-result
  "delete a compilation result from pgmq given the message id"
  [msg-id]
  (delete-pgmq-msg "compilation_results" msg-id))

(comment
  (db/start-pool!)

  (purge-pgmq-queues)

  (publish-pgmq-job {:functions/id (random-uuid)
                     :functions/language "clojure"
                     :functions/source "(println \"Hello, World!\""})

  (clojure.walk/keywordize-keys
   (<-pgobject
    (:message
     (read-from-pgmq "compilation_jobs"))))

  (read-from-pgmq "compilation_jobs")

  (publish-pgmq-message "compilation_results"
                        {:functions/id (random-uuid)
                         :functions/language "clojure"
                         :functions/source "(println \"Hello, World!\""
                         :functions/status "success"
                         :functions/wasm_binary "00"})

  (read-from-pgmq "compilation_results")

  (read-pgmq-result)

  (delete-pgmq-result (get (read-from-pgmq "compilation_results") :msg-id))

  (read-pgmq-result)

  (for [i (range 10)]
    (publish-pgmq-message "compilation_results"
                          {:functions/id (random-uuid)
                           :functions/language "clojure"
                           :functions/source "(println \"Hello, World!\")"
                           :functions/status "success"
                           :functions/wasm_binary "00"}))

  ;; eval that repeatedly within a sec and msg-id's should be invisible for a while (or observe an increment of 5), then show up again after
  (read-pgmq-results-batch 5 2)

  (build-compilation-job {:functions/id (random-uuid)
                          :functions/language "clojure"
                          :functions/source "(println \"Hello, World!\")"
                          :functions/status "pending"})

  )
