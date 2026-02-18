(ns api.pgmq
  (:require
   [next.jdbc :as jdbc :refer [execute! execute-one! execute-batch!]]
   [next.jdbc.sql :refer [insert! update!]]
   [taoensso.telemere :as t :refer [log!]]
   [hikari-cp.core :as hcp]
   [api.db :as db :refer [get-pool]]
   [api.utils :as u :refer [throw-error!]]))

(defn- ->job [id language source]
  #{:functionId id
    :language language
    :source  source})

(defn purge-pgmq-queues []
  (try
    (doseq [queue ["compilation_jobs" "compilation_results"]]
      (execute! (get-pool) ["SELECT pgmq.purge_queue(?)" queue]))
    (log! {:level :debug
           :msg "Purged pgmq queues"})
    (catch Exception e
      (throw-error! ::pgmq-queue-purges-failed e))))

(defn publish-pgmq-job
  "publish a compilation job to pgmq, given the function map"
  [{:functions/keys [id language source]}]
  (try
    (let [result (execute! (get-pool) "SELECT pgmq.send(?, ?)" :compilation-jobs (->job id language source))]
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

  )

