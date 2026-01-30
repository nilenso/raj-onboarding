(ns api.db
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [next.jdbc.types :refer [as-other]]))

(def db {:dbtype "postgres" :dbname "projectnil" :user "projectnil" :password "projectnil"})

(def ds (jdbc/get-datasource db))

(defn get-fns []
  (jdbc/execute! ds ["select * from functions;"]))

(defn add-fn [fn-map]
  (sql/insert! ds :functions fn-map))


(comment

  (add-fn {:name "name"
           :description "desc"
           :language "asc"
           :source "source"
           :status (as-other "PENDING")})

  (get-fns))
