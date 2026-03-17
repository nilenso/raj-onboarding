(ns client
  (:require
   [goose.brokers.redis.broker :as rb]
   [goose.client :as c]))

(defn my-fn [x y]
  (println "called my-fn with " x y))

(def producer
  (let [url  "redis://localhost:6379"
        pool-opts {:max-total-per-key 10
                   :max-idle-per-key 10
                   :min-idle-per-key 2}
        conn-opts {:url url :pool-opts pool-opts}]
    (rb/new-producer conn-opts)))

(defn sentinel []
  (let [client-opts (assoc c/default-opts :broker producer)]
    (c/perform-async client-opts `my-fn "foo" :bar)
    (c/perform-in-sec client-opts 10 `my-fn "foo" :bar)))

(sentinel)

(rb/close producer)
