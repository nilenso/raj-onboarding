(ns api.db-test
  (:require
   [api.db :as api-db]
   [next.jdbc :as jdbc ]
   [next.jdbc.sql :as sql]
   [next.jdbc.types :refer [as-other]]
   [clojure.test :as t :refer [testing is deftest use-fixtures]]))

(def ^:dynamic *datasource* nil)

(use-fixtures :each (fn [test-fn]
                      (let [datasource (api-db/get-profile-datasource :test)]
                        (api-db/truncate-all-tables datasource)
                        (binding [*datasource* datasource]
                          (test-fn)))))

(deftest add-function-test 
  (testing "adding a function to the database"
    (let [datasource *datasource*]
      (api-db/add-function datasource
                           {:name "echo"
                            :description "echos input, unaltered"
                            :language "assemblyscript"
                            :source "export function handle(input: string): string {return input;}"
                            :status (as-other "PENDING")})
      (let [functions (jdbc/execute! datasource ["SELECT * FROM FUNCTIONS WHERE name = ?" "echo"])]
        (is (= 1 (count functions)))
        (is (= "echo" (:functions/name (first functions))))))))

(deftest get-functions-test
  (testing "retrieving functions from the database"
    (let [datasource *datasource*]
      (api-db/add-function datasource
                           {:name "func1"
                            :description "first function"
                            :language "python"
                            :source "def handle(input): return input"
                            :status (as-other "PENDING")})
      (api-db/add-function datasource
                           {:name "func2"
                            :description "second function"
                            :language "javascript"
                            :source "exports.handle = function(input) { return input; }"
                            :status (as-other "PENDING")})
      (let [functions (api-db/get-functions datasource)]
        (is (= 2 (count functions)))
        (is (= #{"func1" "func2"} (set (map :functions/name functions))))))))
