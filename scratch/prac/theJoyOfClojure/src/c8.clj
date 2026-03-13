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


(defmacro unless [condition & clauses]
  `(when (not ~condition)
     ~@clauses))

(unless (even? 3)
  "3 isn't even")

(filter #(apply > (map % [0 1]))
        (for [x (range 2)
              y (range 2)]
          [x y]))

(comment
  (awhen eval-bindee
    do-one
    do-two)) 

(defmacro qualified-it-bad-awhen [expr & body]
  `(let [it ~expr]
     (when it
       ~@body)))
(comment
  (let [user/it 10]
    it)
  ;; this is what that expands to
  ;; try and eval that
  )

(defmacro awhen [expr & body]
  `(let [~'it ~expr]
     (when ~'it
       ~@body)))

(awhen (+ 1 2)
  it)

;; macroexpansion time varying nested binds
(awhen 1
  (awhen (+ it 2)
    [it]))
