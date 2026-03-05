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
      (log! {:level :debug
             :msg "Fetched all executions"
             :data {:execution-count (count executions)}})
      (r/content-type
       (r/response (write-str executions))
       "application/json"))
    (catch Exception e
      (throw-error! ::get-executions-failed e))))

(defn get-function-executions-handler
  "handler for GET /functions/:id/executions, which retrieves all executions for a given function id"
  [req]
  (try
    (let [fn-id (some-> req :path-params :id)
          executions (db/get-function-executions (u/uuidfy fn-id))]
      (log! {:level :debug
             :msg "Fetched executions for function"
             :data {:fn-id fn-id
                    :execution-count (count executions)}})
      (r/content-type
       (r/response (write-str executions))
       "application/json"))
    (catch Exception e
      (throw-error! ::get-function-executions-failed e))))
