(ns api.main
  (:require
   [api.db :as db]
   [taoensso.telemere :as t :refer [log!]]
   [api.gateway :as g]
   [api.utils :as u :refer [throw-error!]]))

(defn -main [& args]
  (u/process-cli-args args)
  (let [http-port (-> @u/configs
                      (:api-server)
                      (:http-port))]
    (db/start-pool!)
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(do (log! {:level :info
                          :msg "Shutting down db connection pool"})
                   (db/stop-pool!))))

    ;; conditionally start the nREPL server if the configuration is set 
    (u/env-predicated-nrepl-init @u/configs :api-server)

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
