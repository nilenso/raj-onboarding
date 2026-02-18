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

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (write-str x)))))

(defn- ->job [id language source]
  {:functionId id
   :language language
   :source  source})

(comment
  (->pgobject (->job ..)))

(defn- <-result []
  nil)

(defn purge-pgmq-queues []
  (try
    (let [results (mapv #(execute! (get-pool) ["SELECT pgmq.purge_queue(?)" %]) ["compilation_jobs" "compilation_results"])]
      (log! {:level :debug
             :msg "Purged pgmq queues"})
      results)
    (catch Exception e
      (throw-error! ::pgmq-queue-purges-failed e))))

(defn publish-pgmq-job
  "publish a compilation job to pgmq, given the function map"
  [{:functions/keys [id language source]}]
  (try
    (let [result (execute-one! (get-pool) ["SELECT pgmq.send(?, ?)"  "compilation_jobs" (->pgobject (->job id language source))])]
      (log! {:level :debug
             :msg "Published job to pgmq"
             :data {:functionId id
                    :language language
                    :source source}})
      result)
    (catch Exception e
      (throw-error! ::pgmq-publish-failed e))))



(comment
  (db/start-pool!)

  (purge-pgmq-queues)

  (publish-pgmq-job {:functions/id (random-uuid)
                     :functions/language "clojure"
                     :functions/source "(println \"Hello, World!\""})


  (->pgobject (->job (random-uuid) "clojure" "(println \"Hello, World!\""))

  )

