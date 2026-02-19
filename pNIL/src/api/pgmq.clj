(ns api.pgmq
  (:require
   [next.jdbc :as jdbc :refer [execute! execute-one! execute-batch!]]
   [clojure.data.json :as json :refer [write-str read-str]]
   [next.jdbc.sql :refer [insert! update!]]
   [taoensso.telemere :as t :refer [log!]]
   [hikari-cp.core :as hcp]
   [api.db :as db :refer [get-pool]]
   [api.utils :as u :refer [throw-error!]])
  (:import
   [org.postgresql.util PGobject]))

(defn- ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (write-str x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure data."
  [^PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (some-> value read-str (with-meta {:pgtype type}))
      value)))

(defn- ->job [id language source]
  {:functionId id
   :language language
   :source  source})

(defn- result []
  nil)

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
    (let [result (execute-one! (get-pool) ["SELECT pgmq.send(?, ?)"  queue (->pgobject fn-map)])]
      (log! {:level :debug
             :msg "Published job to pgmq"
             :data {:queue queue
                    :fn-map fn-map
                    :result result}})
      result)
    (catch Exception e
      (throw-error! ::pgmq-publish-failed e))))

(defn publish-pgmq-job
  "publish a job to pgmq, given a function map"
  [fn-map]
  (assert (and (:functions/id fn-map)
               (:functions/language fn-map)
               (:functions/source fn-map))
          "fn-map contains id, language and source keys")
  (publish-pgmq-message "compilation_jobs" fn-map))

(defn read-one-from-pgmq
  "read a compilation result from pgmq"
  [queue]
  (try
    (let [result (execute-one! (get-pool) ["SELECT * FROM pgmq.read(?, ?::integer, ?::integer, ?::jsonb)" queue 0 1 (->pgobject {})])]
      (log! {:level :debug
             :msg "Read from pgmq"
             :data {:queue queue
                    :result result}})
      (when result
        (clojure.walk/keywordize-keys (<-pgobject (:message result)))))
    (catch Exception e
      (throw-error! ::pgmq-read-failed e {:queue queue}))))

(defn read-pgmq-result
  "read a compilation result from pgmq"
  []
  (read-one-from-pgmq "compilation_results"))

(comment
  (db/start-pool!)

  (purge-pgmq-queues)

  (publish-pgmq-job {:functions/id (random-uuid)
                     :functions/language "clojure"
                     :functions/source "(println \"Hello, World!\""})

  
  (clojure.walk/keywordize-keys
   (<-pgobject
    (:message
     (read-one-from-pgmq "compilation_jobs"))))

  (publish-pgmq-message "compilation_results"
                        {:functions/id (random-uuid)
                         :functions/language "clojure"
                         :functions/source "(println \"Hello, World!\""
                         :functions/status "success"
                         :functions/wasm-bin "00"})

  (read-one-from-pgmq "compilation_results")

  (read-one-from-pgmq "compilation_jobs")

  (read-pgmq-result)

  (->pgobject (->job (random-uuid) "clojure" "(println \"Hello, World!\"")))
