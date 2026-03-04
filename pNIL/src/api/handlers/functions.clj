(ns api.handlers.functions
  (:require
   [api.db :as db]
   [clojure.data.json :refer [read-str write-str]]
   [api.utils :as u :refer [throw-error!]]
   [ring.util.response :as r]
   [taoensso.telemere :as t :refer [log!]]))

;; keys to mask from function data before responding to client
(def ^:private function-data-maskees [:wasm_binary
                                      :source
                                      :compile_error])

;; required keys for a function registration request; extra keys will be ignored, but these must be present
(def ^:private required-function-keys #{:source
                                        :language
                                        :name
                                        :description})

;; valid languages for function registration; this is derived from the implemented languages in the configs, but we can also hardcode it here if we want to be more strict about it
(def ^:private valid-languages (:implemented-languages @u/configs))

(defn- sanitize-function-data
  "mask certain keys from the function data before responding to client, to avoid sending large binary data or potentially sensitive compile error messages"
  [function-data]
  (apply dissoc
         (update-keys function-data (comp keyword name))
         function-data-maskees))

(defn- coerce-function-map
  "check that the required keys are present in the function data; coerce if extra"
  [function-data]
  (if (s/subset? required-function-keys (set (keys function-data)))
    (select-keys function-data required-function-keys)
    (throw-error! ::invalid-function-data
                  (ex-info "Invalid function data: missing required keys" {:function-data function-data})
                  {:reason "missing required keys in function data"
                   :required-keys required-function-keys
                   :provided-keys (set (keys function-data))})))

(defn- valid-function-language?
  "takes the fn-map and returns the fn-map if the language is valid, otherwise throws an error"
  [fn-map]
  (if (valid-languages (keyword (:language fn-map)))
    fn-map
    (throw-error! ::invalid-function-language
                  (ex-info "Invalid function language"
                           {:language (:language fn-map)
                            :valid-languages valid-languages})
                  {:reason "invalid function language"
                   :valid-languages valid-languages
                   :provided-language (:language fn-map)})))

(defn- build-fn-map
  "build a function map from an input stream"
  [istream]
  (update-keys
   (read-str (slurp istream))
   keyword))

(defn- respond-erroneous-request
  "build a bad request response from an exception, including the message and data from the exception if present"
  [err]
  (r/bad-request {:error (ex-message err)
                  :data (ex-data err)}))
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
