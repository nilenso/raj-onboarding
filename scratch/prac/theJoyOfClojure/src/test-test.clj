(ns test-test
  (:require
   [clojure.test :refer [deftest is run-tests]]))

(deftest test-passing-test
  (is true))

(deftest test-failing-test
  (is nil))
