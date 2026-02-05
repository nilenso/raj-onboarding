(ns api.db-test
  (:require
   [api.db :as api-db]
   [next.jdbc :as jdbc]
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

(deftest delete-function-test
  (testing "deleting a function from the database"
    (let [datasource *datasource*
          fn-entry (api-db/add-function datasource
                                        {:name "tobedeleted"
                                         :description "to be deleted"
                                         :language "ruby"
                                         :source "def handle(input); input; end"
                                         :status (as-other "PENDING")})
          fn-id (:functions/id fn-entry)]
      (api-db/delete-function datasource fn-id)
      (let [functions (jdbc/execute! datasource ["SELECT * FROM FUNCTIONS WHERE id = ?" fn-id])]
        (is (= 0 (count functions)))))))
