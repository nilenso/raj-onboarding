(ns api.db
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [next.jdbc.types :refer [as-other]]))

(def db {:dbtype "postgres" :dbname "projectnil" :user "projectnil" :password "projectnil"})

(defn yield-ds [db-spec]
  (jdbc/get-datasource db-spec))

(def ds (yield-ds db))

(defn get-fns [ds]
  (jdbc/execute! ds ["SELECT * FROM FUNCTIONS;"]))

(defn truncate-all-tables [ds]
  (jdbc/execute! ds ["TRUNCATE FUNCTIONS, EXECUTIONS;"]))

(defn add-fn [ds fn-map]
  (sql/insert! ds :functions fn-map))

(comment

  (add-fn ds {:name "name"
              :description "desc"
              :language "asc"
              :source "source"
              :status (as-other "PENDING")})

  (get-fns ds)

  (truncate-all-tables ds)

  )
