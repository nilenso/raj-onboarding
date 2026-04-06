(ns c11
  (:require [clojure.core.reducers :as r]
            [clojure.math :as m]
            [criterium.core :as crit])
  (:import [java.time LocalTime]))

(comment
  future
  promise
  deliver)

(def mapv-futures
  (mapv #(future (do (Thread/sleep (* % 100))
                             (str (LocalTime/now))))
        (for [_ (range 10)]
          (rand-int 10))))

(mapv deref mapv-futures)

(comment
  pcalls
  pvalues
  pmap
  doall
  r/fold)


(for [power (range 0 20)]
  (let [reductee (repeat  (m/pow 2 power) 1)]
    (print "fold :")
    (time (r/fold + reductee))
    (print "reduce :")
    (time (reduce + reductee))))

;;   fold :"Elapsed time: 0.367625 msecs"
;;   reduce :"Elapsed time: 0.00175 msecs"
;;   fold :"Elapsed time: 0.009833 msecs"
;;   reduce :"Elapsed time: 0.001208 msecs"
;;   fold :"Elapsed time: 0.003542 msecs"
;;   reduce :"Elapsed time: 0.0015 msecs"
;;   fold :"Elapsed time: 0.007042 msecs"
;;   reduce :"Elapsed time: 0.002417 msecs"
;;   fold :"Elapsed time: 0.005583 msecs"
;;   reduce :"Elapsed time: 0.009541 msecs"
;;   fold :"Elapsed time: 0.008417 msecs"
;;   reduce :"Elapsed time: 0.007166 msecs"
;;   fold :"Elapsed time: 0.016833 msecs"
;;   reduce :"Elapsed time: 0.014625 msecs"
;;   fold :"Elapsed time: 0.029125 msecs"
;;   reduce :"Elapsed time: 0.031583 msecs"
;;   fold :"Elapsed time: 0.058542 msecs"
;;   reduce :"Elapsed time: 0.066167 msecs"
;;   fold :"Elapsed time: 0.036041 msecs"
;;   reduce :"Elapsed time: 0.026 msecs"
;;   fold :"Elapsed time: 0.045875 msecs"
;;   reduce :"Elapsed time: 0.076625 msecs"
;;   fold :"Elapsed time: 0.114375 msecs"
;;   reduce :"Elapsed time: 0.109084 msecs"
;;   fold :"Elapsed time: 0.1915 msecs"
;;   reduce :"Elapsed time: 0.224791 msecs"
;;   fold :"Elapsed time: 0.409708 msecs"
;;   reduce :"Elapsed time: 0.455666 msecs"
;;   fold :"Elapsed time: 0.935084 msecs"
;;   reduce :"Elapsed time: 0.912833 msecs"
;;   fold :"Elapsed time: 2.607917 msecs"
;;   reduce :"Elapsed time: 1.344125 msecs"
;;   fold :"Elapsed time: 5.2755 msecs"
;;   reduce :"Elapsed time: 0.552667 msecs"
;;   fold :"Elapsed time: 3.344083 msecs"
;;   reduce :"Elapsed time: 0.970875 msecs"
;;   fold :"Elapsed time: 3.4215 msecs"
;;   reduce :"Elapsed time: 0.569083 msecs"
;;   fold :"Elapsed time: 11.7995 msecs"
;;   reduce :"Elapsed time: 1.276208 msecs"


(for [power (range 0 20)]
  (let [reductee (vec (repeat  (m/pow 2 power) 1))]
    (print "fold :")
    (time (r/fold + reductee))
    (print "reduce :")
    (time (reduce + reductee))))

;;   fold :"Elapsed time: 0.046459 msecs"
;;   reduce :"Elapsed time: 0.005208 msecs"
;;   fold :"Elapsed time: 0.007708 msecs"
;;   reduce :"Elapsed time: 0.001208 msecs"
;;   fold :"Elapsed time: 0.00525 msecs"
;;   reduce :"Elapsed time: 0.001083 msecs"
;;   fold :"Elapsed time: 0.0045 msecs"
;;   reduce :"Elapsed time: 9.58E-4 msecs"
;;   fold :"Elapsed time: 0.004791 msecs"
;;   reduce :"Elapsed time: 0.001292 msecs"
;;   fold :"Elapsed time: 0.004917 msecs"
;;   reduce :"Elapsed time: 0.001541 msecs"
;;   fold :"Elapsed time: 0.00675 msecs"
;;   reduce :"Elapsed time: 0.002 msecs"
;;   fold :"Elapsed time: 0.007833 msecs"
;;   reduce :"Elapsed time: 0.002584 msecs"
;;   fold :"Elapsed time: 0.010959 msecs"
;;   reduce :"Elapsed time: 0.004375 msecs"
;;   fold :"Elapsed time: 0.024542 msecs"
;;   reduce :"Elapsed time: 0.005917 msecs"
;;   fold :"Elapsed time: 0.130583 msecs"
;;   reduce :"Elapsed time: 0.011916 msecs"
;;   fold :"Elapsed time: 0.120125 msecs"
;;   reduce :"Elapsed time: 0.016584 msecs"
;;   fold :"Elapsed time: 0.203459 msecs"
;;   reduce :"Elapsed time: 0.029708 msecs"
;;   fold :"Elapsed time: 0.506042 msecs"
;;   reduce :"Elapsed time: 0.061083 msecs"
;;   fold :"Elapsed time: 0.450917 msecs"
;;   reduce :"Elapsed time: 0.09725 msecs"
;;   fold :"Elapsed time: 0.471125 msecs"
;;   reduce :"Elapsed time: 0.203958 msecs"
;;   fold :"Elapsed time: 0.632125 msecs"
;;   reduce :"Elapsed time: 0.38675 msecs"
;;   fold :"Elapsed time: 0.829209 msecs"
;;   reduce :"Elapsed time: 0.666834 msecs"
;;   fold :"Elapsed time: 1.2735 msecs"
;;   reduce :"Elapsed time: 1.3785 msecs"
;;   fold :"Elapsed time: 2.026708 msecs"
;;   reduce :"Elapsed time: 5.582375 msecs"


(defn costly-+
  ([a b]
   (Thread/sleep 1) (+ a b))
  ([a] (costly-+ a 0))
  ([] (costly-+ 0 0)))


(for [power (range 0 20)]
  (let [reductee (vec (repeat  (m/pow 2 power) 1))]
    (print "fold :")
    (time (r/fold costly-+ reductee))
    (print "reduce :")
    (time (reduce costly-+ reductee))))

;;   fold : "Elapsed time: 2.578167 msecs"
;;   reduce : "Elapsed time: 0.003416 msecs"
;;   fold : "Elapsed time: 3.798042 msecs"
;;   reduce : "Elapsed time: 1.266084 msecs"
;;   fold : "Elapsed time: 6.316417 msecs"
;;   reduce : "Elapsed time: 3.7775 msecs"
;;   fold : "Elapsed time: 11.330416 msecs"
;;   reduce : "Elapsed time: 8.809166 msecs"
;;   fold : "Elapsed time: 21.4 msecs"
;;   reduce : "Elapsed time: 18.871333 msecs"
;;   fold : "Elapsed time: 41.429458 msecs"
;;   reduce : "Elapsed time: 39.0165 msecs"
;;   fold : "Elapsed time: 81.81725 msecs"
;;   reduce : "Elapsed time: 79.300375 msecs"
;;   fold : "Elapsed time: 162.384 msecs"
;;   reduce : "Elapsed time: 160.056708 msecs"
;;   fold : "Elapsed time: 324.362917 msecs"
;;   reduce : "Elapsed time: 321.835041 msecs"
;;   fold : "Elapsed time: 645.249416 msecs"
;;   reduce : "Elapsed time: 647.245959 msecs"
;;   fold : "Elapsed time: 647.969541 msecs"
;;   reduce : "Elapsed time: 1291.737792 msecs"
;;   fold : "Elapsed time: 649.009375 msecs"
;;   reduce : "Elapsed time: 2582.427166 msecs"
;;   fold : "Elapsed time: 653.176 msecs"
;;   reduce : "Elapsed time: 5177.084334 msecs"
;;   fold : "Elapsed time: 1297.284208 msecs"
;;   reduce : "Elapsed time: 10334.894458 msecs"
;;   fold : "Elapsed time: 1952.009166 msecs"
;;   reduce : "Elapsed time: 20680.128125 msecs"
;;   fold : "Elapsed time: 3883.327917 msecs"
;;   reduce : "Elapsed time: 41358.132583 msecs"
;;   fold : "Elapsed time: 7135.018917 msecs"
;;   "interrupted ...



(comment
  checking out criterium/bench)


(comment
  crit/bench
  crit/quick-bench)

(defn costly-+
  [delay]
  (fn
    ([] 0)
    ([a] a)
    ([a b]
     (Thread/sleep delay)
     (+ a b))))

(def c-+ (costly-+ 1))

(c-+ 0)

(c-+ 0 0)

(crit/quick-bench (r/fold 8 c-+ c-+ (vec (range 64))))
;; Evaluation count : 48 in 6 samples of 8 calls.
;; Execution time mean : 14.068862 ms
;; Execution time std-deviation : 100.785246 µs
;; Execution time lower quantile : 13.936233 ms ( 2.5%)
;; Execution time upper quantile : 14.165010 ms (97.5%)
;; Overhead used : 1.563152 ns

(crit/quick-bench (reduce c-+ (vec (range 64))))
;; Evaluation count : 12 in 6 samples of 2 calls.
;; Execution time mean : 79.888860 ms
;; Execution time std-deviation : 328.942557 µs
;; Execution time lower quantile : 79.489228 ms ( 2.5%)
;; Execution time upper quantile : 80.280069 ms (97.5%)
;; Overhead used : 1.563152 ns
