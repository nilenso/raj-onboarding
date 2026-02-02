(ns api.db
  (:require
   [next.jdbc :as jdbc :refer [execute!]]
   [next.jdbc.sql :as sql :refer [insert!]]
   [next.jdbc.types :refer [as-other]]))


(defn get-functions [datasource]
  (execute! datasource ["SELECT * FROM FUNCTIONS;"]))

(defn truncate-all-tables [datasource]
  (execute! datasource ["TRUNCATE FUNCTIONS, EXECUTIONS;"]))

(defn add-function [datasource fn-map]
  (insert! datasource :functions fn-map))

(comment

  (def db-spec {:dbtype "postgres"
                :dbname "projectnil"
                :user "projectnil"
                :password "projectnil"
                :host "localhost"
                :port 5432}) ; 5433 when testing

  (def datasource (jdbc/get-datasource db-spec))

  (truncate-all-tables datasource)

  (add-function datasource {:name "name"
                            :description "desc"
                            :language "asc"
                            :source "source"
                            :status (as-other "PENDING")})

  (get-functions datasource)

  (truncate-all-tables datasource))
