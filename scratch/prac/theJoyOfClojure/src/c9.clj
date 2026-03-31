(ns c9)

(defmulti compiler :os)
(defmethod compiler ::unix [m] (get m :c-compiler))

(compiler {:os ::unix :c-compiler "gcc"})

(compiler {:os ::osx :c-compiler "clang"})

(defrecord TreeNode [val l r])



((juxt :val :l :r)
 (TreeNode. 0
            (TreeNode. 1 nil nil)
            nil))

(comment
  extend
  extend-protocol
  extend-type)

(defprotocol StringOps
  (rev [s])
  (upp [s]))

(extend-type String
  StringOps
  (rev [s] (clojure.string/reverse s))
  (upp [s] (clojure.string/upper-case s)))

((juxt rev upp) "test")
