(ns api.db
  (:require
   [next.jdbc :as jdbc :refer [execute! get-datasource]]
   [next.jdbc.sql :as sql :refer [insert!]]
   [next.jdbc.types :as types]
   [taoensso.telemere :as t :refer [log! error!]]
   [utils :as u :refer [prog1]]))

(defn get-profile-datasource
  "create a next.jdbc datasource for the given profile (:dev or :test)"
  [profile]
  (let [base-spec (:db u/configs)
        port (-> base-spec
                 (:ports)
                 (profile))]
    (get-datasource
     (assoc base-spec
            :port port))))

(defn truncate-all-tables
  "truncate all tables in the database"
  [datasource]
  (try
    (prog1
     (execute! datasource ["TRUNCATE FUNCTIONS, EXECUTIONS;"])
     (log! :debug "All tables truncated successfully"))
    (catch Exception e
      (error! ::truncate-failed e)
      (throw e))))

(defn get-functions
  "retrieve all functions from the FUNCTIONS table"
  [datasource]
  (try
    (prog1 
     (execute! datasource ["SELECT * FROM FUNCTIONS;"])
     (log! :debug "Functions retrieved successfully"))
    (catch Exception e
      (error! ::function-retrieval-failed e)
      (throw e))))

(defn add-function
  "insert a new function into the FUNCTIONS table"
  [datasource fn-map]
  (try 
    (prog1
     (insert! datasource :functions fn-map)
     (log! :debug ::function-addition-successful fn-map))
    (catch Exception e
      (error! ::function-addition-failed e {:function-name (:name fn-map)})
      (throw e))))

(defn delete-function
  "delete a function from the FUNCTIONS table by id"
  [datasource fn-id]
  (try
    (prog1 
     (execute! datasource ["DELETE FROM FUNCTIONS WHERE id = ?;" fn-id])
     (log! :debug ::function-deletion-successful {:function-id fn-id}))
    (catch Exception e
      (error! ::function-deletion-failed e {:function-id fn-id})
      (throw e))))
