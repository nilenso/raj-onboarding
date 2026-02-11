(ns api.utils
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [clojure.tools.cli :refer [parse-opts]]
   [nrepl.server :as nrepl]
   [taoensso.telemere :as t :refer [log! error!]]
   [cider.nrepl :refer [cider-nrepl-handler]]))

(defn env-predicated-nrepl-init
  "run the nrepl server if :nrepl? is true in configs"
  [configs component]
  (if (:nrepl? configs)
    (do
      (log! :info "starting :nrepl")
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
  [] (read-configs "config.edn" "secrets.edn")
  [config-path  secrets-path]
  (try
    (let [base-config (read-config (io/resource config-path))
          secrets (read-config (io/resource secrets-path))]
      (log! {:level :info 
             :msg "Configs read from resources" 
             :data {:base-config base-config :secrets (keys secrets)}})
      (merge base-config secrets))
    (catch Exception e
      (error! ::config-read-failed e)
      (throw e))))

(def cli-options
  [["-c" "--configs CONFIG" "Path to config file in resources"
    :default "config.edn"]
   ["-s" "--secrets SECRETS" "Path to secrets file in resources"
    :default "secrets.edn"]
   ["-h" "--help"]])

(defonce configs (read-configs))
