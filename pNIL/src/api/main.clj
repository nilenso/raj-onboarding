(ns api.main
  (:require
   [api.db :as db]
   [api.poller :as p]
   [taoensso.telemere :as t :refer [log!]]
   [api.gateway :as g]
   [api.utils :as u :refer [throw-error!]]))

(defn -main [& args]

  (u/process-cli-args args)

  
  ;; init nrepl if configured
  (u/env-predicated-nrepl-init @u/configs :api-server)

  ;; start common db conn pool and register shutdown hook
  (db/start-pool!)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(db/stop-pool!)))

  ;; start poller and register shutdown hook
  (let [poller-config (-> @u/configs
                          (:api-server)
                          (:poller))
        stop-chan (p/start-poller poller-config)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(p/stop-poller stop-chan))))

  ;; start api gateway and register shutdown hook
  (let [http-port (-> @u/configs
                      (:api-server)
                      (:http-port))]
    (try
      (let [stop-server-fn (g/run-server http-port)]
        (.addShutdownHook
         (Runtime/getRuntime)
         (Thread. #(do (log! {:level  :info
                              :msg "Shutting down API server"
                              :data {:http-port http-port}})
                       (stop-server-fn)))))
      (catch Exception e
        (db/stop-pool!)
        (throw-error! ::server-start-failed e)))))
