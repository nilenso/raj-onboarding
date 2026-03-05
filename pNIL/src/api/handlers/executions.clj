(ns api.handlers.executions
  (:require
   [api.db :as db]
   [api.pgmq :as q]
   [clojure.data.json :refer [read-str write-str]]
   [clojure.core.async :refer [thread]]
   [clojure.set :as s]
   [api.utils :as u :refer [throw-error!]]
   [ring.util.response :as r]
   [taoensso.telemere :as t :refer [log!]]))

(defn get-executions-handler
  "handler for GET /executions, returns a list of all executions in the system"
  [request]
  (try
    (let [executions (db/get-executions)]
      (log! {:level :info
             :msg "Fetched all executions"
             :data {:execution-count (count executions)}})
      (r/content-type
       (r/response (write-str executions))
       "application/json"))
    (catch Exception e
      (throw-error! ::get-executions-failed e))))
