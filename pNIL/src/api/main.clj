(ns api.main
  (:require
   [org.httpkit.server :as hk-server]
   [reitit.ring :as ring]
   [taoensso.telemere :as t :refer [log! error!]]
   [utils :as u]))

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

(defn -main []
  (let [http-port (-> u/configs
                      (:api-server)
                      (:http-port))]
    (u/env-predicated-nrepl-init u/configs :api-server)
    (try
      (hk-server/run-server app {:port http-port})
      (log! {:level :info
             :msg "API server is running"
             :data {:http-port http-port}})
      (catch Exception e
        (error! ::server-start-failed e)))))
