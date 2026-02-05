(ns utils
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [nrepl.server :as nrepl]
   [taoensso.telemere :as t :refer [log! error!]]
   [cider.nrepl :refer [cider-nrepl-handler]]))

(defn env-predicated-nrepl-init
  "maybe run the nrepl server conditioned on the current runtime environment"
  [configs component]
  (if (= (:env configs) "DEV")
    (do
      (log! :info "DEV environment detected")
      (try
        (let [nrepl-port (-> configs
                             (component)
                             (:nrepl-port))]
          (nrepl/start-server :port nrepl-port
                              :handler cider-nrepl-handler)
          (log! {:level :info 
                 :msg "nrepl server started" 
                 :data {:component component :nrepl-port nrepl-port}}))
        (catch Exception e
          (error! {:id ::nrepl-server-start-failed :data {:component component}} e))))
    (log! {:level :info 
           :msg "Non-DEV environment - skipping nrepl startup" 
           :data {:component component :env (:env configs)}})))

(defn- read-configs 
  "read configs, secrets from resources and merge them"
  []
  (try
    (let [base-config (read-config (io/resource "config.edn"))
          secrets (read-config (io/resource "secrets.edn"))]
      (log! {:level :info 
             :msg "Configs read from resources" 
             :data {:base-config base-config :secrets (keys secrets)}})
      (merge base-config secrets))
    (catch Exception e
      (error! ::config-read-failed e)
      (throw e))))

(defonce configs (read-configs))

(defmacro prog1
  "Evaluate expr1, then evaluate and return the value of exprs...
   (prog1 expr1 expr2 expr3 ...)
   Returns the value of expr1."
  [expr1 & exprs]
  `(let [result# ~expr1]
     ~@exprs
     result#))
