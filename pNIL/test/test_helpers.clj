(ns test-helpers
  (:require  [clojure.test :as t]))

(defn thrown-with-id? [expected-id f]
  (try
    (f)
    false
    (catch clojure.lang.ExceptionInfo e
      (= expected-id (:id (ex-data e))))))
