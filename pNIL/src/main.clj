(ns main
  (:require [org.httpkit.server :as hk-server]
            [nrepl.server :as nrepl]
            [reitit.ring :as ring]))

(defn- root-handler [_req]
  {:status  200
   :body "Sentinel Body"
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
  (println  "starting httpkit-server")
  (hk-server/run-server app {:port 8080})
  (println  "starting nrepl-server")
  (nrepl/start-server :port 8081))
