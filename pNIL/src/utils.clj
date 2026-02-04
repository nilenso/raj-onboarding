(ns utils
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [nrepl.server :as nrepl]))

(defn env-predicated-nrepl-init [configs component]
  "Runs the nrepl server conditioned on the current runtime environment"
  (when (= (:env configs)
           "DEV")
    (let [nrepl-port (-> configs
                         (component)
                         (:nrepl-port))]
      (println "starting" component "nrepl | port" nrepl-port)
      (nrepl/start-server :port nrepl-port)
      (println component "nrepl started"))))

(defn read-config-file [path]
  "Reads the config file at the given path and returns it as a map"
  (read-config (io/resource path)))

