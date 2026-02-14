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

(defn read-configs 
  "read configs, secrets from resources and merge them"
  ([] (read-configs "config.edn" "secrets.edn"))
  ([config-path  secrets-path]
   (try
     (let [base-config (read-config (io/resource config-path))
           secrets (read-config (io/resource secrets-path))]
       (log! {:level :info 
              :msg "Configs read from resources"})
       (log! {:level :debug
              :msg "config contents"
              :data {:base base-config
                     :secrets secrets}} )
       (merge base-config secrets))
     (catch Exception e
       (error! ::config-read-failed e)
       (throw e)))))

(def cli-options
  [["-c" "--configs-file CONFIG" "Path to config file in resources"
    :default "config.edn"]
   ["-s" "--secrets-file SECRETS" "Path to secrets file in resources"
    :default "secrets.edn"]
   ["-l" "--log-level LEVEL" "Logging level (e.g. :debug, :info, :warn, :error)"
    :parse-fn keyword
    :default :info]
   ["-h" "--help"]])

(defonce configs (atom nil))

(defn process-cli-args
  "process command line arguments to override default config paths"
  [args]
  (let [options (:options (parse-opts args cli-options))
        cfile (:configs-file options)
        sfile (:secrets-file options)
        log-level (:log-level options)]
    (t/set-min-level! :log log-level)
    (log! {:level :info
           :msg "CLI args processed"
           :data {:configs-file cfile
                  :secrets-file sfile
                  :log-level log-level}})
    (reset! configs (read-configs cfile sfile))))
