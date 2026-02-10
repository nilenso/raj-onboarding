(ns c5
  (:require
   [clojure.repl :refer [doc]]
   [clojure.java.javadoc :refer [javadoc]]))

(def j javadoc)

(doc into-array)

(def ds (into-array [1 2 3]))

ds

(seq ds)

(j (class (seq ds)))

(doc aset)

(aset ds 1 0)

(seq ds)

(doc type)

(def dsi [:a :b :c])

dsi

(def dsir (replace {:a :d} dsi))

dsir

(doc replace)

(doc transduce)

;; https://clojure.org/reference/transducers

;;  l-mhd : cider-doc
;;  l-mhj : cider-javadoc

;; l-mhd clojure.test

;; testing cider progress bar
(Thread/sleep 5000)


(j (class (hash-map :a 1)))

(doc into)

(doc transduce)

(into [1 2] (map inc) [2 3])

(doc comp)

((comp #(take 5 %) #(map inc %) #(range %)) 10)

(doc eduction)

(eduction (comp (map inc) (map #(* % %))) (range 10))
