(ns api.db
  (:require
   [next.jdbc :as jdbc :refer [execute! get-datasource]]
   [next.jdbc.sql :as sql :refer [insert!]]
   [next.jdbc.types :as types]
   [utils :as u]))

(defn get-profile-datasource [profile]
  (let [base-spec (:db u/configs)
        port (-> base-spec
                 (:ports)
                 (profile))]
    (get-datasource
     (assoc base-spec
            :port port))))

(defn get-functions [datasource]
  (execute! datasource ["SELECT * FROM FUNCTIONS;"]))

(defn truncate-all-tables [datasource]
  (execute! datasource ["TRUNCATE FUNCTIONS, EXECUTIONS;"]))

(defn add-function [datasource fn-map]
  (insert! datasource :functions fn-map))

(comment
  (def datasource (get-profile-datasource :dev))               ; :test or :dev

  (truncate-all-tables datasource)

  (add-function datasource {:name "name"
                            :description "desc"
                            :language "asc"
                            :source "source"
                            :status (types/as-other "PENDING")})

  (get-functions datasource)

  (truncate-all-tables datasource))
