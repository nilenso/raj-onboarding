(ns api.handlers.functions
  (:require
   [api.db :as db]
   [api.pgmq :as q]
   [clojure.data.json :refer [read-str write-str]]
   [clojure.core.async :refer [thread]]
   [clojure.set :as s]
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
(defn post-function-handler
  "handler for POST /functions endpoint, which registers a new function and sends it to the compiler via pgmq"
  [req]
  (try
    (let [fn-data (build-fn-map (:body req))
          db-ack (db/add-function (-> fn-data
                                      coerce-function-map
                                      valid-function-language?))
          fn-id (:functions/id db-ack)]
      (log! {:level :debug
             :id ::post-function-handler-successful
             :data {:req-fn-body fn-data
                    :fn-id (:functions/id db-ack)
                    :fn-name (:functions/name db-ack)}})
      (thread
        (try
          (db/update-function fn-id {:status "compiling"})
          (q/publish-pgmq-job db-ack)
          (log! {:level :debug
                 :id ::pgmq-publish-successful
                 :data {:fn-id fn-id
                        :fn-name (:functions/name db-ack)}})
          ;; TODO : retry mechanism with exp backoff?
          (catch Exception e
            (log! {:level :error
                   :id ::pgmq-publish-failed
                   :data {:fn-id fn-id
                          :fn-name (:functions/name db-ack)
                          :error (ex-message e)}}))))
      (r/created (str "/functions/" fn-id)
                 (write-str (sanitize-function-data db-ack))))
    (catch Exception e
      (respond-erroneous-request e))))

(defn get-functions-handler
  "handler for GET /functions endpoint, which retrieves all functions from the database and returns them to the client, after sanitizing the data to mask certain keys"
  [req]
  (let [functions (db/get-functions)]
    (log! {:level :debug
           :id ::get-functions-handler-called
           :data {:num-functions (count functions)}})
    (r/content-type
     (r/response (write-str functions) )
     "application/json")))

(defn get-function-by-id-handler
  "handler for GET /functions/:id endpoint, which retrieves a function by id from the database and returns it to the client, after sanitizing the data to mask certain keys"
  [req]
  (let [fn-id (some-> req :path-params :id)
        function (sanitize-function-data (db/get-function-by-id (u/uuidfy fn-id)))]
    (if (not (empty? function))
      (do
        (log! {:level :debug
               :id ::get-function-by-id-handler-successful
               :data {:fn-id fn-id}})
        (r/content-type
         (r/response (write-str function))
         "application/json"))
      (do
        (log! {:level :debug
               :id ::get-function-by-id-handler-not-found
               :data {:fn-id fn-id}})
        (respond-erroneous-request (ex-info "Function not found" {:fn-id fn-id}))))))

(defn delete-function-handler
  "handler for DELETE /functions/:id endpoint, which deletes a function by id from the database"
  [req]
  (let [fn-id (some-> req :path-params :id)]
    (try
      (db/delete-function (u/uuidfy fn-id))
      (log! {:level :debug
             :id ::delete-function-handler-successful
             :data {:fn-id fn-id}})
      (r/response (str "Function with id " fn-id " deleted successfully"))
      (catch Exception e
        (log! {:level :error
               :id ::delete-function-handler-failed
               :data {:fn-id fn-id
                      :error (ex-message e)}})
        (r/bad-request {:error "Failed to delete function"})))))


(comment

  (sanitize-function-data {:functions/id #uuid "123e4567-e89b-12d3-a456-426614174000"
                           :functions/name "test-fn"
                           :functions/language "clojure"
                           :functions/source "(println \"Hello, World!\")"
                           :functions/description "a test function"
                           :functions/status "pending"
                           :functions/wasm_binary (byte-array [0 1 2 3])
                           :functions/compile_error nil})

  (coerce-function-map {:name "test-fn"
                        :language "clojure"
                        :source "(println \"Hello, World!\")"
                        :description "a test function"})

  (coerce-function-map {:name "test-fn"
                        :language "clojure"
                        :source "(println \"Hello, World!\""})

  (coerce-function-map {:name "test-fn"
                        :language "clojure"
                        :source "(println \"Hello, World!\")"
                        :description "a test function"
                        :extra-key "should be ignored"})

  (valid-function-language? {:language "clojure"})

  (valid-function-language? {:language "assemblyscript"}))
