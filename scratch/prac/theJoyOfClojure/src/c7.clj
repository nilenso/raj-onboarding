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

(let  [a :a
       b :b
       c :c]
  (letfn [(sa [[_ & r]]
            (case _
              nil true
              :a false
              :b (sb r)
              :c (sc r)))
          (sb [[_ & r]]
            (case _
              nil true
              :b false
              :a (sa r)
              :c (sc r)))
          (sc [[_ & r]]
            (case _
              nil true
              :c false
              :a (sa r)
              :b (sb r)))]
    [(trampoline sb [:a :b :c :b :a :c ])
     (trampoline sb [:a :b :c :b :a :c :c])
     (trampoline sa [:b :c :a :c :a :b])]))
