(ns utils
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [nrepl.server :as nrepl]
   [cider.nrepl :refer [cider-nrepl-handler]]))

(defn env-predicated-nrepl-init [configs component]
  "Runs the nrepl server conditioned on the current runtime environment"
  (if (= (:env configs) "DEV")
    (do
      (println "DEV environment detected")
      (let [nrepl-port (-> configs
                           (component)
                           (:nrepl-port))]
        (println "starting" component "nrepl | port" nrepl-port)
        (nrepl/start-server :port nrepl-port
                            :handler cider-nrepl-handler)
        (println component "nrepl started")))
    (println "Non-DEV environment" (:env configs) "detected - skipping" component "nrepl startup")))

(defn- read-configs []
  "read configs, secrets from resources and merge them"
  (let [base-config (read-config (io/resource "config.edn"))
        secrets (read-config (io/resource "secrets.edn"))]
    (merge base-config secrets)))

(defonce configs (read-configs))
