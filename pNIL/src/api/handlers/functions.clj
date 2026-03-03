(ns api.handlers.functions
  (:require
   [api.db :as db]
   [taoensso.telemere :as t :refer [log!]]))

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
