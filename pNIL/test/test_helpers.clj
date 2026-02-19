(ns test-helpers)

(defn thrown-with-id? [expected-id f]
  (try
    (f)
    false
    (catch clojure.lang.ExceptionInfo e
      (= expected-id (:id (ex-data e))))))
