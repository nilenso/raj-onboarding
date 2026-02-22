(ns api.db
  (:require
   [next.jdbc :as jdbc :refer [execute! execute-one!]]
   [next.jdbc.sql :refer [insert! update!]]
   [next.jdbc.types :as jdbc-types :refer [as-other]]
   [clojure.string :as s :refer [upper-case]]
   [taoensso.telemere :as t :refer [log!]]
   [hikari-cp.core :as hcp]
   [api.utils :as u :refer [throw-error!]]))

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
  (or @pool (throw-error! ::pool-not-initialized)))

(defn truncate-all-tables
  "truncate all tables in the database"
  []
  (try
    (let [result (execute! (get-pool) ["TRUNCATE FUNCTIONS, EXECUTIONS;"])]
      (log! :debug ::all-tables-truncated-successfully)
      result)
    (catch Exception e
      (throw-error! ::truncate-failed e))))

(defn- enwrap-pg-status-enum
  "helper function to convert status string to a PG-compatible type"
  [status]
  (as-other (upper-case status)))

(defn get-functions
  "retrieve all functions from the FUNCTIONS table"
  []
  (try
    (let [result (execute! (get-pool) ["SELECT * FROM FUNCTIONS;"])]
      (log! :debug ::functions-retrieved-successfully)
      result)
    (catch Exception e
      (throw-error! ::function-retrieval-failed e))))

(defn get-function-by-id
  "retrieve a function with specific id"
  [fn-id]
  (try
    (let [result (execute-one! (get-pool) ["SELECT * FROM FUNCTIONS WHERE id = ?;" fn-id])]
      (log! {:level :debug
             :id ::get-function-by-id-successful
             :data result})
      result)
    (catch Exception e
      (throw-error! ::get-function-by-id-failed e {:fn-id fn-id}))))

(defn add-function
  "insert a new function into the FUNCTIONS table"
  [fn-map]
  (try
    (let [result (insert! (get-pool) :functions (if (get fn-map :status)
                                                  (assoc fn-map :status
                                                         (enwrap-pg-status-enum (:status fn-map)))
                                                  fn-map))]
      (log! {:level :debug :id ::function-addition-successful :data fn-map})
      result)
    (catch Exception e
      (throw-error!  ::function-addition-failed e {:function-name (:name fn-map)}))))


(defn delete-function
  "delete a function from the FUNCTIONS table by id"
  [fn-id]
  (try
    (let [result (execute! (get-pool) ["DELETE FROM FUNCTIONS WHERE id = ?;" fn-id])]
      (log! {:level :debug :id ::function-deletion-successful :data {:function-id fn-id}})
      result)
    (catch Exception e
      (throw-error! ::function-deletion-failed e {:function-id fn-id}))))

(defn update-function
  "update a function in the FUNCTIONS table"
  [fn-id fn-update-map]
  (when-not (get-function-by-id fn-id)
    (throw-error! ::update-on-non-existent-fn-id nil {:fn-id fn-id}))
  (try
    (update! (get-pool) :functions (if (get fn-update-map :status)
                                     (assoc fn-update-map :status
                                            (enwrap-pg-status-enum (:status fn-update-map)))
                                     fn-update-map)
             {:id fn-id})
    (log! {:level :debug :id ::function-update-successful :data {:fn-id fn-id}})
    (get-function-by-id fn-id)
    (catch Exception e
      (throw-error! ::function-update-failed e {:fn-id fn-id}))))

(comment
  (start-pool!)

  (def test-fn-id (random-uuid))

  (add-function {:id test-fn-id
                 :name "test-fn"
                 :language "clojure"
                 :source "(println \"Hello, World!\")"
                 :status "pending"})

  (get-function-by-id test-fn-id)

  (update-function test-fn-id {:status "ready"})

  (get-function-by-id test-fn-id)

  (delete-function test-fn-id)

  (get-function-by-id test-fn-id)

  )
