(ns api.db
  (:require
   [next.jdbc :as jdbc :refer [execute!]]
   [next.jdbc.sql :as sql :refer [insert!]]
   [taoensso.telemere :as t :refer [log! error!]]
   [hikari-cp.core :as hcp]
   [api.utils :as u]))

(defn- pool-options
  "generate HikariCP options"
  []
  (let [db-spec (:db @u/configs)
        pool-cfg (get-in @u/configs [:api-server :db-cp])]
    (log! {:level :debug
           :id ::pool-options-generated
           :data {:db-spec db-spec
                  :pool-cfg pool-cfg}})
    (merge pool-cfg
           {:adapter       "postgresql"
            :database-name (:dbname db-spec)
            :server-name   (:host db-spec)
            :port-number   (:port db-spec)
            :username      (:username db-spec)
            :password      (:password db-spec)
            :pool-name     (str "pnil")})))

(defonce ^:private pool (atom nil))

(defn start-pool!
  "initialize the connection pool"
  ([]
   (if @pool
     (log! :warn ::pool-already-initialized)
     (do
       (log! :info ::starting-connection-pool)
       (reset! pool (hcp/make-datasource (pool-options)))
       (log! :info ::connection-pool-started)))))

(defn stop-pool!
  "close the connection pool if it exists"
  []
  (when-let [p @pool]
    (log! :info ::stopping-connection-pool)
    (hcp/close-datasource p)
    (reset! pool nil)))

(defn get-pool
  "retrieve the connection pool, throwing an error if it's not initialized"
  []
  (or @pool (throw (ex-info "Pool not initialized" {}))))

(defn truncate-all-tables
  "truncate all tables in the database"
  []
  (try
    (let [result (execute! (get-pool) ["TRUNCATE FUNCTIONS, EXECUTIONS;"])]
      (log! :debug "All tables truncated successfully")
      result)
    (catch Exception e
      (error! ::truncate-failed e)
      (throw e))))

(defn get-functions
  "retrieve all functions from the FUNCTIONS table"
  []
  (try
    (let [result (execute! (get-pool) ["SELECT * FROM FUNCTIONS;"])]
      (log! :debug "Functions retrieved successfully")
      result)
    (catch Exception e
      (error! ::function-retrieval-failed e)
      (throw e))))

(defn get-function-by-id [fn-id]
  nil)
(defn add-function
  "insert a new function into the FUNCTIONS table"
  [fn-map]
  (try
    (let [result (insert! (get-pool) :functions fn-map)]
      (log! {:level :debug :id ::function-addition-successful :data fn-map})
                result )
    (catch Exception e
      (error! {:id ::function-addition-failed :data {:function-name (:name fn-map)}} e)
      (throw e))))

(defn delete-function
  "delete a function from the FUNCTIONS table by id"
  [fn-id]
  (try
    (let [result (execute! (get-pool) ["DELETE FROM FUNCTIONS WHERE id = ?;" fn-id])]
      (log! {:level :debug :id ::function-deletion-successful :data {:function-id fn-id}})
                result )
    (catch Exception e
      (error! {:id ::function-deletion-failed :data {:function-id fn-id}} e)
      (throw e))))
