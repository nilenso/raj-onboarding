(ns c3
  (:require [clojure.repl :as r]
            [clojure.java.javadoc :as j]))

(seq [])

(seq [1 2])

(doseq [x [1 2 3]
        y [2 3 4]]
  (print [y x]))

(r/doc doseq)

(def date-regex #"(\d{1,2})\/(\d{1,2})\/(\d{4})")

(let [rem (re-matcher date-regex  "12/02/1954")]
  (when (.find rem)
    (let [[_ m d] rem]
      {:month m :day d})))

(defn xors [[xr yr]]
  (for [x (range xr)
        y (range yr)]
   [x y  (bit-xor x y)]))

(xors [2 2])

(def frame (java.awt.Frame.))

(for [meth (.getMethods java.awt.Frame)
      :let [name (.getName meth)]
      :when (re-find #"Vis" name)]
  name)

(.setVisible frame true)

(.setSize frame (java.awt.Dimension. 200 200))

(j/javadoc java.awt.Frame)

(def gfx (.getGraphics frame))

(j/javadoc java.awt.Graphics)

(r/doc juxt)
((juxt #(.getHeight %) #(.getWidth %)) frame)

(defn yield-dims [frame]
  ((juxt #(.getHeight %) #(.getWidth %)) frame))

(defmacro getter-juxt [& getters]
  `(juxt ~@(for [g getters] `#(~g %))))

(getter-juxt .getHeight .getWidth)

(defn yield-dims-better [frame]
  ((getter-juxt .getHeight .getWidth) frame))

(yield-dims-better frame)

;; find out the issues that is leading to failure in the getter-juxt macro
(map #(println %) '(1 2))


(doseq [[x y xor] (xors (yield-dims frame))]
  (.setColor gfx (java.awt.Color. xor xor xor))
  (.fillRect gfx x y 1 1))

(.dispose frame)

