(ns c8)

(defn contextual-eval [ctx expr]
  `(let [~@(mapcat (fn [[k v]]
                     [(symbol k) v])
                   ctx)]
     ~expr))

(contextual-eval '{a (inc 1)
                   b (+ a 2)}
                 '(+ a b))

(eval
 (contextual-eval '{a (inc 1)
                    b (+ a 2)}
                  '(+ a b)))



(defmacro do-until [& body]
  {:pre  [(even? (count body))]}
  (when body
    `(when ~(first body)
       ~(second body)
       (do-until ~@(next (next body))))))

(do-until
  (even? 2) (println "even")
  (odd? 3) (println "odd")
  (zero? 1) (println "won't print")
  :truthy (println "truthy"))

