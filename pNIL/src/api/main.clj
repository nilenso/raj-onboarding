(ns api.main
  (:require
   [org.httpkit.server :as hk-server]
   [api.db :as db]
   [reitit.ring :as ring]
   [taoensso.telemere :as t :refer [log!]]
   [api.utils :as u :refer [throw-error!]]))

(defn- root-handler [_req]
  {:status  200 :body "Sentinel Body"
   :headers {"Content-Type" "text/html"}})

(defn- status-handler [_req]
  {:status 200
   :body "OK"
   :headers {"Content-Type" "text/html"}})

(def app
  (ring/ring-handler
   (ring/router [["/" {:get root-handler}]
                 ["/status" {:get status-handler}]])))

(defn -main [& args]
  (u/process-cli-args args)               
  (let [http-port (-> @u/configs
                      (:api-server)
                      (:http-port))]
    (db/start-pool!)
    (.addShutdownHook 
     (Runtime/getRuntime)
     (Thread. #(do (log! :info "Shutting down")
                   (db/stop-pool!))))
    (u/env-predicated-nrepl-init @u/configs :api-server)
    (try
      (hk-server/run-server app {:port http-port})
      (log! {:level :info
             :msg "API server is running"
             :data {:http-port http-port}})
      (catch Exception e
        (db/stop-pool!)
        (throw-error! ::server-start-failed e)))))
