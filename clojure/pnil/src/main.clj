(ns main

  (:require [org.httpkit.server :as hk-server]
            [nrepl.server :as nrepl]))

(defn yield-bod []
  "hello HTTP! changed (changed)")

(defn app [req]
  {:status  200
   :body (yield-bod)
   :headers {"Content-Type" "text/html"}})

(defn -main []
  (println  "starting httpkit-server")
  (hk-server/run-server app {:port 8080})
  (println  "starting nrepl-server")
  (nrepl/start-server :port 8081))
