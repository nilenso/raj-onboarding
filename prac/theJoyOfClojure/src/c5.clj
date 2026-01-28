(ns c5
  (:require [clojure.repl :refer [doc]]
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
