(ns prac)

(defn _+
  ([] 0)
  ([x] x)
  ([x y] (. clojure.lang.Numbers (add x y)))
  ([x y & more]
   (reduce + (+ x y) more)))

(_+ 1 2 3)

(def x 1)
(def y 2)

(let [r 5
      pi 3.1415
      r-squared (* r r)]
  (println "for radius" r)
  (println "area is " (* pi r-squared)))
