(ns api.db-test
  (:require
   [next.jdbc :as jdbc ]
   [next.jdbc.sql :as sql]
   [next.jdbc.types :refer [as-other]]
   [clojure.test :as t :refer [testing is deftest]]))

(def test-db-spec {:dbtype "postgres"
                   :dbname "projectnil"
                   :user "projectnil"
                   :password "projectnil"
                   :host "localhost"
                   :port 5433}) ; 5433 when testing


(def datasource (jdbc/get-datasource test-db-spec))

(deftest add-function
  )
