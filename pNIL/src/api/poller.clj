(ns api.poller
  (:require
   [taoensso.telemere :as t :refer [log!]]
   [clojure.core.async :as a :refer [go-loop timeout chan alts!]]
   [api.handlers.compilations :refer [process-compilation-result]]
   [api.pgmq :as q]
   [api.utils :as u :refer [throw-error!]]))

(defn start-poller [poller-config]
  (log! {:level :info
         :msg "Starting compilation results poller"
         :data {:poller-config poller-config}})
  (let [stop-chan (chan)
        poll-interval-ms (:poll-interval-ms poller-config)
        poll-batch-size (:batch-size poller-config)]
    (go-loop []
      (log! {:level :debug
             :msg "Polling for compilation results"})
      (let [[val port] (alts! [stop-chan (timeout poll-interval-ms)])]
        (if (= port stop-chan)
          (log! {:level :info
                 :msg "received stop signal, exiting poller loop"})
          (do

            ;; poll for results and mapv futures of compilation result handlers
            
            (recur)))))
    stop-chan))

;; close over put to stop-chan to signal the poller loop to exit
;; easier idempotence
(defn stop-poller [stop-chan]
  (a/close! stop-chan))
