(ns clojure.core )
(def p println)
`p

(ns c2
  (:require [clojure.repl :as r]
            [clojure.set :as s]))

(r/doc ns)

(defn tf []
  "test func"
  `test-func)

(p `tf)

(p (s/intersection #{1 2 3} #{2 3 4}))
