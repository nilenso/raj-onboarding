(ns c3
  (:require [clojure.repl :as r]))

(seq [])

(seq [1 2])

(doseq [x [1 2 3]
        y [2 3 4]]
  (print [y x]))

(r/doc doseq)

