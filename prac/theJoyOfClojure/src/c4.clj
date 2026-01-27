(ns c4
  (:require [clojure.repl :refer [doc]]
            [clojure.java.javadoc :refer [javadoc]]))


(def testmap {:a 'a :b 'b})
(:a testmap)

(doc iterate)

(doc range)

(doc take)
 
(doc transduce)

(defn pour [lb ub]
  (cond
    (= ub :toujours) (iterate inc lb)
    :else (range lb ub)))

(pour 0 10)

(take 10 (pour 0 :toujours))

::test-q-key

(defn best [f xs]
  (reduce #(if (f %1 %2) %1 %2) xs))

(best > (range 10))
(best < (range 10))

