(ns c7 )

(str (let [f "foo"] 
       (reify Object
         (toString [this] f))))

(seq (let [f "foo"] 
       (reify clojure.lang.Seqable
         (seq [this] (seq f)))))

(defprotocol TestProtocol
  (only-true [this whatever] "generates only true")
  (only-false [this] "generates only false"))

(only-true
 (let [x 1]
   (reify TestProtocol
     ;; 'this' represents the reified object
     (only-true [this whatever] true)
     (only-false [this] false)))
 'whatever)
