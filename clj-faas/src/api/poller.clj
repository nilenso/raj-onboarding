(ns api.poller
  (:require
   [taoensso.telemere :as t :refer [log!]]
   [clojure.core.async :as a :refer [go-loop timeout chan alts! pipeline]]
   [api.handlers.compilations :as c]
   [api.pgmq :as q]))

(defn start-poller
  [{:keys [poll-interval-ms
           visibilty-timeout-sec
           batch-size
           worker-count]}]
  (log! {:level :info
         :msg "Starting compilation results poller"
         :data {:poll-interval-ms poll-interval-ms
                :visibilty-timeout-sec visibilty-timeout-sec
                :batch-size batch-size
                :worker-count worker-count}})
  (let [stop-chan (chan)
        work-chan (chan batch-size)     
        out-chan (chan)]                

    ;; start worker pipeline to process compilation results
    (pipeline worker-count
              out-chan                  
              (comp (map c/process) ;transducer application order : map runs first
                    (filter (constantly false))) ;don't need the results, not populating out-chan
              work-chan) 
    
    (go-loop []
      (comment (log! {:level :debug
                      :msg "Polling for compilation results"}))
      (let [[_val port] (alts! [stop-chan (timeout poll-interval-ms)])]
        (if (= port stop-chan)
          (do (log! {:level :info
                     :msg "received stop signal, exiting poller loop"})
              (a/close! work-chan))
          (do
            (try
              (let [messages (q/peek-results-batch batch-size visibilty-timeout-sec)]
                (doseq [msg messages]
                  (a/>! work-chan msg))) ;async put that parks when work-chan full (backpressure)
              (catch Exception e
                (log! {:level :error
                       :id ::poller-loop-error
                       :data {:error e}})))
            (recur)))))
    stop-chan))

;; close over put to stop-chan to signal the poller loop to exit
;; easier idempotence
(defn stop-poller [stop-chan]
  (a/close! stop-chan))
