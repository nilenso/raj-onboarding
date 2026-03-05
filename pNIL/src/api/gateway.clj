(ns api.gateway
  (:require
   [reitit.ring :as r]
   [taoensso.telemere :as t :refer [log!]]
   [org.httpkit.server :as hks]
   [api.handlers.functions :as f]
   [api.handlers.executions :as e]
   [api.handlers.core :as ahc]))

(def app
  (r/ring-handler
   (r/router [["/status" {:get ahc/status-handler}]
              ["/functions" {:get f/get-functions-handler
                             :post f/post-function-handler}]
              ["/functions/:id" {:get f/get-function-by-id-handler
                                 :delete f/delete-function-handler}]
              ["/functions/:id/executions" {:get e/get-function-executions-handler}]
              ["/executions" {:get e/get-executions-handler}]])))

(defn run-server
  "Starts the API server on the specified HTTP port."
  [http-port]
  (let [stop-fn (hks/run-server app {:port http-port})]
    (log! {:level :info
           :msg "API server started successfully"
           :data {:http-port http-port}})
    stop-fn))



(comment

  ;; -main server runs on 9080
  ;; using this for quick iterations
  (def stop-fn (run-server 9079))

  (stop-fn)

  )
