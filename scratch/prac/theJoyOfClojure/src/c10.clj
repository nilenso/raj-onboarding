(ns c10
  (:import java.util.concurrent.Executors))

(comment
  dosync)

(def thread-pool
  (Executors/newFixedThreadPool
   (+ 2 (.availableProcessors (Runtime/getRuntime)))))

(defn dothreads!
  [f & {thread-count :threads
        exec-count :times
        :or {thread-count 1 exec-count 1}}]
  (dotimes [t thread-count]
    (.submit thread-pool
             #(dotimes [_ exec-count] (f)))))


(dothreads! #(println "Hi ") :threads 2 :times 8)

(comment
  io!)

(comment
  java.lang.IllegalStateException
  RuntimeException)

(comment
  dosync
  commute
  ensure
  ref-set
  alter)

(comment
  agent
  send
  send-off
  send-via)

(def a (agent []))

(send a conj :a)

(send a (fn [_] []))

(let [slow-conj (fn [coll item time-ms]
                  (Thread/sleep time-ms)
                  (conj coll item))]
  (send a slow-conj :a 2000)
  (future (send a slow-conj :b 2000))
  (send a slow-conj :c 200))

(comment
  agent-error
  restart-agent)

(comment
  atom)

(defn manipulable-memoize [function]
  (let [cache (atom {})]
    (with-meta
      (fn [& args]
        (or (second (find @cache args))
            (let [ret (apply function args)]
              (swap! cache assoc args ret)
              ret)))
      {:cache cache})))


(defn- fib-iter [a b n]
  (if (zero? n)
    a
    (recur b (+ a b) (dec n))))

(defn- fib [n]
  (fib-iter 0 1 n))

(def fib-memoized (manipulable-memoize fib))

(mapv #(time (fib-memoized %)) [20 20 20])

(comment
  "Elapsed time: 0.036458 msecs"
  "Elapsed time: 0.005541 msecs"
  "Elapsed time: 0.00275 msecs")

(meta fib-memoized)

(def memoized-fib-iter (manipulable-memoize fib-iter))

(defn fib-finer-memoized [n]
  (memoized-fib-iter 0 1 n))

(mapv #(time (fib-finer-memoized %)) [20 20 20])

(comment
  "Elapsed time: 0.075792 msecs"
  "Elapsed time: 0.012458 msecs"
  "Elapsed time: 0.003625 msecs")

(meta memoized-fib-iter)

;; memoized recurs via mundane calls
(defn- fib-iter-mundane [a b n]
  (if (zero? n)
    a
    (fib-iter-mundane b (+ a b) (dec n))))

(def fib-iter-mundane-memoized 
  (manipulable-memoize fib-iter-mundane))

(defn fib-finer-mundane-memoized [n]
  (fib-iter-mundane-memoized 0 1 n))

(mapv #(time (fib-finer-mundane-memoized %)) [20 20 20])
(comment
  "Elapsed time: 0.079458 msecs"
  "Elapsed time: 0.008958 msecs"
  "Elapsed time: 0.003125 msecs")

(meta fib-iter-mundane-memoized)


(declare fib-granular) 

(def fib-granular
  (manipulable-memoize
   (fn [a b n]
     (if (zero? n)
       a
       (fib-granular b (+ a b) (dec n)))))) 

(mapv #(time (fib-granular 0 1 %)) [20 20 20])

(comment
  "Elapsed time: 0.136584 msecs"
  "Elapsed time: 0.028042 msecs"
  "Elapsed time: 0.013084 msecs")

(meta fib-granular)
