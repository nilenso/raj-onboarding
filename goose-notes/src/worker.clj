(ns worker
  (:require
   [goose.brokers.redis.broker :as rb]
   [goose.worker :as w]))


(def consumer (rb/new-consumer rb/default-opts))

(def worker
  (let [worker-opts (assoc w/default-opts :broker consumer)]
    (w/start worker-opts)))

(w/stop worker)

(rb/close consumer)
