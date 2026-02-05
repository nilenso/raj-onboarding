(ns api.db
  (:require
   [next.jdbc :as jdbc :refer [execute!]]
   [next.jdbc.sql :as sql :refer [insert!]]
   [taoensso.telemere :as t :refer [log! error!]]
   [hikari-cp.core :as hcp]
   [utils :as u :refer [prog1]]))

(defn- pool-options
  "generate HikariCP options for the given profile (:dev or :test)"
  [profile]
  (let [db-spec (:db u/configs)
        pool-cfg (get-in u/configs [:api-server :db-cp])
        port (get-in db-spec [:ports profile])]
    (merge pool-cfg
           {:adapter       "postgresql"
            :database-name (:dbname db-spec)
            :server-name   (:host db-spec)
            :port-number   port
            :username      (:username db-spec)
            :password      (:password db-spec)
            :pool-name     (str "pnil-" (name profile))})))

(defonce ^:private pool (atom nil))

(defn start-pool!
  "initialize the connection pool for the given profile (:dev or :test)"
  ([] (start-pool! :dev))
  ([profile]
   (if @pool
     (log! :warn ::pool-already-initialized)
     (do
       (log! :info ::starting-connection-pool)
       (reset! pool (hcp/make-datasource (pool-options profile)))
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
    (prog1
     (execute! (get-pool) ["TRUNCATE FUNCTIONS, EXECUTIONS;"])
     (log! :debug "All tables truncated successfully"))
    (catch Exception e
      (error! ::truncate-failed e)
      (throw e))))

(defn get-functions
  "retrieve all functions from the FUNCTIONS table"
  []
  (try
    (prog1
     (execute! (get-pool) ["SELECT * FROM FUNCTIONS;"])
     (log! :debug "Functions retrieved successfully"))
    (catch Exception e
      (error! ::function-retrieval-failed e)
      (throw e))))

(defn add-function
  "insert a new function into the FUNCTIONS table"
  [fn-map]
  (try
    (prog1
     (insert! (get-pool) :functions fn-map)
     (log! {:level :debug :id ::function-addition-successful :data fn-map}))
    (catch Exception e
      (error! {:id ::function-addition-failed :data {:function-name (:name fn-map)}} e)
      (throw e))))

(defn delete-function
  "delete a function from the FUNCTIONS table by id"
  [fn-id]
  (try
    (prog1
     (execute! (get-pool) ["DELETE FROM FUNCTIONS WHERE id = ?;" fn-id])
     (log! {:level :debug :id ::function-deletion-successful :data {:function-id fn-id}}))
    (catch Exception e
      (error! {:id ::function-deletion-failed :data {:function-id fn-id}} e)
      (throw e))))
