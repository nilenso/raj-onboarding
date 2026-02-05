(ns api.db-test
  (:require
   [api.db :as api-db]
   [next.jdbc :as jdbc ]
   [next.jdbc.sql :as sql]
   [next.jdbc.types :refer [as-other]]
   [clojure.test :as t :refer [testing is deftest]]))

;; make fixture


;; make fixture
(def datasource (api-db/get-profile-datasource :test))

;; pre-exec fixture

(deftest add-function-test
  (testing "adding a function to the database"
    (api-db/truncate-all-tables datasource)

    (api-db/add-function datasource
                         {:name "echo"
                          :description "echos input, unaltered"
                          :language "assemblyscript"
                          :source "export function handle(input: string): string {return input;}"
                          :status (as-other "PENDING")})

    (let [functions (jdbc/execute! datasource ["SELECT * FROM FUNCTIONS WHERE name = ?" "echo"])]
      (is (= 1 (count functions)))
      (is (= "echo" (:functions/name (first functions)))))))
