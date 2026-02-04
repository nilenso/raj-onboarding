(ns api.db-test
  (:require
   [api.db :refer [add-function]]
   [next.jdbc :as jdbc ]
   [next.jdbc.sql :as sql]
   [next.jdbc.types :refer [as-other]]
   [clojure.test :as t :refer [testing is deftest]]))

;; make fixture
(def test-db-spec {:dbtype "postgres"
                   :dbname "projectnil"
                   :user "projectnil"
                   :password "projectnil"
                   :host "localhost"
                   :port 5433}) ; 5433 when testing


;; make fixture
(def datasource (jdbc/get-datasource test-db-spec))

;; pre-exec fixture

(deftest add-function-test
  (testing "adding a function to the database"
    (add-function datasource
                  {:name "echo"
                   :description "echos input, unaltered"
                   :language "assemblyscript"
                   :source "export function handle(input: string): string {return input;}"
                   :status (as-other "PENDING")})
    (let [functions (jdbc/execute! datasource ["SELECT * FROM FUNCTIONS WHERE name = ?" "echo"])]
      (is (= 1 (count functions)))
      (is (= "echo" (:name (first functions)))))))
