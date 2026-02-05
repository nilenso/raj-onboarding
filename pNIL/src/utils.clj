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
          (log! :info "nrepl server started port"
                {:component component :nrepl-port nrepl-port}))
        (catch Exception e
          (error! ::nrepl-server-start-failed e {:component component}))))
    (log! :info "Non-DEV environment detected - skipping nrepl startup" {:component component :env (:env configs)})))

(defn- read-configs 
  "read configs, secrets from resources and merge them"
  []
  (try
    (let [base-config (read-config (io/resource "config.edn"))
          secrets (read-config (io/resource "secrets.edn"))]
      (log! :info "Configs read from resources"
            {:base-config base-config :secrets (keys secrets)})
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
