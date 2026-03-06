(ns api.handlers.executions
  (:require
   [api.db :as db]
   [api.wasm :as w]
   [clojure.data.json :refer [read-str write-str]]
   [api.utils :as u :refer [throw-error! respond-erroneous-request]]
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

(defn execute-function-handler
  "handler for POST /functions/:id/execute, which initiates an execution for a given function id.
   Always returns 200 — FAILED status represents user-code failure, not a server error."
  [req]
  (let [fn-id (u/uuidfy (some-> req :path-params :id))
        input (some-> req :body slurp read-str)
        input-map (get input "input" {})
        input-json (write-str input-map)
        function (db/get-function-by-id fn-id)]
    ;; validate function exists
    (when-not function
      (throw-error! ::function-not-found nil {:fn-id fn-id}))
    ;; validate function is READY
    (let [status (:functions/status function)]
      (when-not (= "READY" status)
        (throw-error! ::function-not-ready nil {:fn-id fn-id :status status})))
    ;; create execution record
    (let [wasm-binary (:functions/wasm_binary function)
          timeout-ms (-> @u/configs
                         (:api-server)
                         (:wasm)
                         (:timeout-ms))
          runtime (w/make-runtime timeout-ms)
          execution (db/add-execution {:function_id fn-id
                                       :input input-map
                                       :status "running"
                                       :started_at  (java.time.LocalDateTime/now)})
          ex-id (:executions/id execution)]
      (try
        (let [output-json (w/execute runtime wasm-binary input-json)
              output-map (read-str output-json)
              updated (db/update-execution ex-id {:status "completed"
                                                  :output output-map
                                                  :completed_at (java.time.LocalDateTime/now)})]
          (log! {:level :info
                 :id ::execution-completed
                 :data {:ex-id ex-id :fn-id fn-id}})
          (r/content-type
           (r/response (write-str updated))
           "application/json"))
        (catch Exception e
          (let [updated (db/update-execution ex-id {:status "failed"
                                                    :error_message (ex-message e)
                                                    :completed_at (java.time.LocalDateTime/now)})]
            (log! {:level :warn
                   :id ::execution-failed
                   :data {:ex-id ex-id :fn-id fn-id :error (ex-message e)}})
            (r/content-type
             (r/response (write-str updated))
             "application/json")))))))

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

(defn get-execution-by-id-handler
  "handler for GET /executions/:id, which retrieves an execution by id"
  [req]
  (try
    (let [execution-id (some-> req :path-params :id)
          execution (db/get-execution-by-id (u/uuidfy execution-id))]
      (if execution
        (do
          (log! {:level :debug
                 :msg "Fetched execution by id"
                 :data {:execution-id execution-id}})
          (r/content-type
           (r/response (write-str execution))
           "application/json"))
        (do
          (log! {:level :debug
                 :msg "Execution not found"
                 :data {:execution-id execution-id}})
          (respond-erroneous-request (ex-info "Execution not found" {:execution-id execution-id})))))))
