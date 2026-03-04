(ns api.handlers.functions
  (:require
   [api.db :as db]
   [clojure.data.json :refer [read-str write-str]]
   [api.utils :as u :refer [throw-error!]]
   [ring.util.response :as r]
   [taoensso.telemere :as t :refer [log!]]))

(defn- sanitize-function-data [function-data]
  (dissoc 
   (update-keys function-data (comp keyword name))
   :wasm_binary
   :source
   :compile_error))

(defn post-function-handler [req]
  (try 
    (let [fn-data (read-str (slurp (:body req)))
          db-ack (db/add-function fn-data)
          fn-id (:functions/id db-ack)]
      (log! {:level :debug
             :id ::post-function-handler-successful
             :data {:req-fn-body fn-data
                    :fn-id (:functions/id db-ack)
                    :fn-name (:functions/name db-ack)}})
      (r/created (str "/functions/" fn-id) (write-str (sanitize-function-data db-ack))))
    (catch Exception e
      (throw-error! ::post-function-handler-failed e))))

(defn get-functions-handler [req]
  (let [functions  (map #(update-keys (dissoc % ;;unqualify keys
                                              :wasm_binary)
                                      (comp keyword name))
                        (db/get-functions))]
    (log! {:level :debug
           :id ::get-functions-handler-called
           :data {:num-functions (count functions)}})
    {:status 200
     :body functions
     :headers {"Content-Type" "application/json"}}))
