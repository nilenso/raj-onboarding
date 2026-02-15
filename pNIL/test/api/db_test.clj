(ns api.db-test
  (:require
   [api.db :as api-db]
   [next.jdbc :as jdbc]
   [next.jdbc.types :as jdbc-types]
   [api.utils :as u]
   [test-helpers :as h]
   [clojure.test :as t :refer [testing is deftest use-fixtures]]))

(use-fixtures :once 
  (fn [f]
    (u/process-cli-args ["-c" "config.edn"
                         "-s" "secrets-test.edn"])
    (api-db/start-pool!)
    (try (f)
         (finally (api-db/stop-pool!)))))

(use-fixtures :each (fn [f]
                      (api-db/truncate-all-tables)
                      (f)))

(deftest add-function-test
  (testing "adding a function to the database"
    (api-db/add-function {:name "echo"
                          :description "echos input, unaltered"
                          :language "assemblyscript"
                          :source "export function handle(input: string): string {return input;}"
                          :status (jdbc-types/as-other "PENDING")})
    (let [functions (jdbc/execute! (api-db/get-pool) ["SELECT * FROM FUNCTIONS WHERE name = ?" "echo"])]
      (is (= 1 (count functions)))
      (is (= "echo" (:functions/name (first functions)))))))

(deftest get-functions-test
  (testing "retrieving functions from the database"
    (api-db/add-function {:name "func1"
                          :description "first function"
                          :language "python"
                          :source "def handle(input): return input"
                          :status (jdbc-types/as-other "PENDING")})
    (api-db/add-function {:name "func2"
                          :description "second function"
                          :language "javascript"
                          :source "exports.handle = function(input) { return input; }"
                          :status (jdbc-types/as-other "PENDING")})
    (let [functions (api-db/get-functions)]
      (is (= 2 (count functions)))
      (is (= #{"func1" "func2"} (set (map :functions/name functions)))))))

(deftest delete-function-test
  (testing "deleting a function from the database"
    (let [fn-entry (api-db/add-function {:name "tobedeleted"
                                         :description "to be deleted"
                                         :language "ruby"
                                         :source "def handle(input); input; end"
                                         :status (jdbc-types/as-other "PENDING")})
          fn-id (:functions/id fn-entry)]
      (api-db/delete-function fn-id)
      (let [functions (jdbc/execute! (api-db/get-pool) ["SELECT * FROM FUNCTIONS WHERE id = ?" fn-id])]
        (is (= 0 (count functions)))))))

(deftest get-function-by-id-test
  (testing "retrieving a function by id:"
    (testing "non existent id retreival:"
      (is (= nil (api-db/get-function-by-id (random-uuid)))))
    (testing "newly created function retrieval"
      (let [fn-1 (api-db/add-function {:name "func1"
                                       :description "first function"
                                       :language "python"
                                       :source "def handle(input): return input"
                                       :status (jdbc-types/as-other "PENDING")})
            fn-1-id (:functions/id fn-1)
            fn-2 (api-db/add-function {:name "func2"
                                       :description "second function"
                                       :language "javascript"
                                       :source "exports.handle = function(input) { return input; }"
                                       :status (jdbc-types/as-other "PENDING")})
            fn-2-id (:functions/id fn-2)]
        (is (= fn-1-id (:functions/id (api-db/get-function-by-id fn-1-id))))
        (is (= fn-2-id (:functions/id (api-db/get-function-by-id fn-2-id))))))))


(deftest update-function-test
  (testing "throws when updating non-existent id"
    (is (h/thrown-with-id? :api.db/update-on-non-existent-fn-id
                           #(api-db/update-function (random-uuid) {:name "x"}))))
  (testing "updates reflected when fetching again"
    (let [original (api-db/add-function {:name "original-name"
                                         :description "original-desc"
                                         :language "assemblyscript"
                                         :source "original-source"
                                         :status (jdbc-types/as-other "PENDING")})
          fn-id (:functions/id original)
          _ (api-db/update-function fn-id {:name "updated-name"
                                           :description "updated-desc"})
          fetched (api-db/get-function-by-id fn-id)]
      (is (= "updated-name" (:functions/name fetched)))
      (is (= "updated-desc" (:functions/description fetched)))
      ;; unchanged fields remain
      (is (= "assemblyscript" (:functions/language fetched)))
      (is (= "original-source" (:functions/source fetched))))))
