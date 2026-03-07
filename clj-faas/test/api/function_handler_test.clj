(ns api.function-handler-test
  "Integration tests for function-related HTTP handlers.
   Tests the full flow: HTTP request → handler → DB → response."
  (:require
   [api.db :as db]
   [api.handlers.functions :as fn-handler]
   [api.utils :as u]
   [clojure.data.json :refer [read-str write-str]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [taoensso.telemere :as tel]))

(use-fixtures :once
  (fn [f]
    (tel/set-min-level! :fatal)
    (u/process-cli-args ["-c" "config.edn"
                         "-s" "secrets-test.edn"])
    (db/start-pool!)
    (try (f)
         (finally (db/stop-pool!)))))

(use-fixtures :each
  (fn [f]
    (db/truncate-all-tables)
    (f)))

(defn- make-post-request
  "Build a ring-style POST request with a JSON body."
  [body-map]
  {:body (io/input-stream (.getBytes (write-str body-map) "UTF-8"))})

(defn- make-get-request
  "Build a ring-style GET request with path params."
  [params]
  {:path-params params})

(defn- parse-json-body
  "Parse a JSON string response body to Clojure data."
  [response]
  (read-str (:body response)))

;; ---- post-function-handler ----

(deftest post-valid-function-returns-201
  (testing "posting a valid function returns 201 with the created function data"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "echo"
                                        "description" "echoes input"
                                        "language" "assemblyscript"
                                        "source" "export function handle(input: string): string { return input; }"}))
          body (parse-json-body response)]
      (is (= 201 (:status response)))
      (is (some? (get body "id")))
      (is (= "echo" (get body "name")))
      (is (= "assemblyscript" (get body "language")))
      (is (= "echoes input" (get body "description"))))))

(deftest post-function-masks-sensitive-fields
  (testing "response body excludes wasm_binary, source, and compile_error"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "secret-fn"
                                        "description" "has secrets"
                                        "language" "assemblyscript"
                                        "source" "super secret source code"}))
          body (parse-json-body response)]
      (is (= 201 (:status response)))
      (is (nil? (get body "wasm_binary")))
      (is (nil? (get body "source")))
      (is (nil? (get body "compile_error"))))))

(deftest post-function-missing-required-keys-returns-400
  (testing "missing name returns 400"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"description" "no name"
                                        "language" "assemblyscript"
                                        "source" "some source"}))]
      (is (= 400 (:status response)))))
  (testing "missing source returns 400"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "no-source"
                                        "description" "missing source"
                                        "language" "assemblyscript"}))]
      (is (= 400 (:status response)))))
  (testing "missing language returns 400"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "no-lang"
                                        "description" "missing language"
                                        "source" "some source"}))]
      (is (= 400 (:status response)))))
  (testing "missing description returns 400"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "no-desc"
                                        "language" "assemblyscript"
                                        "source" "some source"}))]
      (is (= 400 (:status response))))))

(deftest post-function-invalid-language-returns-400
  (testing "a language not in :implemented-languages returns 400"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "py-fn"
                                        "description" "python function"
                                        "language" "python"
                                        "source" "def handle(input): return input"}))]
      (is (= 400 (:status response))))))

(deftest post-function-extra-keys-ignored
  (testing "extra keys beyond the required set are stripped before insertion"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "extra-fn"
                                        "description" "has extra keys"
                                        "language" "assemblyscript"
                                        "source" "some source"
                                        "extra_key" "should be ignored"
                                        "another" 123}))
          body (parse-json-body response)]
      (is (= 201 (:status response)))
      (is (nil? (get body "extra_key")))
      (is (nil? (get body "another"))))))

(deftest post-function-persists-to-db
  (testing "function is retrievable from DB after posting"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {"name" "persisted-fn"
                                        "description" "should persist"
                                        "language" "assemblyscript"
                                        "source" "export function handle(input: string): string { return input; }"}))
          body (parse-json-body response)
          fn-id (get body "id")
          fetched (db/get-function-by-id (parse-uuid fn-id))]
      (is (= 201 (:status response)))
      (is (some? fetched))
      (is (= "persisted-fn" (:functions/name fetched)))
      (is (= "assemblyscript" (:functions/language fetched))))))

;; ---- get-functions-handler ----

(deftest get-functions-empty-returns-200
  (testing "GET /functions with no functions returns 200 with empty array"
    (let [response (fn-handler/get-functions-handler {})
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (= [] body)))))

(deftest get-functions-returns-sanitized-list
  (testing "GET /functions returns all functions with sanitized fields"
    (db/add-function {:name "fn-a" :description "first" :language "assemblyscript"
                      :source "source-a" :status "READY"})
    (db/add-function {:name "fn-b" :description "second" :language "assemblyscript"
                      :source "source-b" :status "PENDING"})
    (let [response (fn-handler/get-functions-handler {})
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (= 2 (count body)))
      ;; sanitize-function keeps only id, status, name, description
      (is (every? #(contains? % "name") body))
      (is (every? #(nil? (get % "source")) body))
      (is (every? #(nil? (get % "wasm_binary")) body)))))

;; ---- get-function-by-id-handler ----

(deftest get-function-by-id-existing-returns-200
  (testing "GET /functions/:id for an existing function returns 200 with sanitized data"
    (let [func (db/add-function {:name "findme" :description "find me"
                                 :language "assemblyscript"
                                 :source "my source" :status "READY"})
          fn-id (:functions/id func)
          response (fn-handler/get-function-by-id-handler
                    (make-get-request {:id (str fn-id)}))
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (= (str fn-id) (get body "id")))
      (is (= "findme" (get body "name")))
      ;; source is masked by sanitize-function-data
      (is (nil? (get body "source")))
      (is (nil? (get body "wasm_binary")))
      (is (nil? (get body "compile_error"))))))

(deftest get-function-by-id-nonexistent-returns-400
  (testing "GET /functions/:id for a non-existent UUID returns 400"
    (let [response (fn-handler/get-function-by-id-handler
                    (make-get-request {:id (str (random-uuid))}))]
      (is (= 400 (:status response))))))

(deftest get-function-by-id-invalid-uuid-returns-400
  (testing "GET /functions/:id with a non-UUID string returns 400 (uuidfy returns nil, function not found)"
    (let [response (fn-handler/get-function-by-id-handler
                    (make-get-request {:id "not-a-uuid"}))]
      (is (= 400 (:status response))))))

;; ---- delete-function-handler ----

(deftest delete-existing-function-returns-200
  (testing "DELETE /functions/:id for an existing function returns 200"
    (let [func (db/add-function {:name "deleteme" :description "to delete"
                                 :language "assemblyscript"
                                 :source "source" :status "PENDING"})
          fn-id (:functions/id func)
          response (fn-handler/delete-function-handler
                    (make-get-request {:id (str fn-id)}))]
      (is (= 200 (:status response))))))

(deftest delete-function-removes-from-db
  (testing "function is no longer retrievable after deletion"
    (let [func (db/add-function {:name "will-vanish" :description "ephemeral"
                                 :language "assemblyscript"
                                 :source "source" :status "PENDING"})
          fn-id (:functions/id func)]
      (fn-handler/delete-function-handler (make-get-request {:id (str fn-id)}))
      (is (nil? (db/get-function-by-id fn-id))))))

(deftest delete-nonexistent-function-returns-200
  (testing "DELETE /functions/:id for a non-existent UUID still returns 200 (DELETE is idempotent)"
    (let [response (fn-handler/delete-function-handler
                    (make-get-request {:id (str (random-uuid))}))]
      (is (= 200 (:status response))))))

(deftest delete-function-with-executions-cascades
  (testing "deleting a function also removes its executions"
    (let [func (db/add-function {:name "with-execs" :description "has executions"
                                 :language "assemblyscript"
                                 :source "source" :status "READY"})
          fn-id (:functions/id func)]
      (db/add-execution {:function_id fn-id :input {:x 1} :status "COMPLETED"})
      (db/add-execution {:function_id fn-id :input {:x 2} :status "FAILED"})
      (is (= 2 (count (db/get-function-executions fn-id))))
      (fn-handler/delete-function-handler (make-get-request {:id (str fn-id)}))
      (is (= 0 (count (db/get-executions)))))))

;; ---- gap: post-function-handler with completely empty body {} → 400 ----

(deftest post-function-empty-body-returns-400
  (testing "posting {} with no keys at all returns 400 (all required keys missing)"
    (let [response (fn-handler/post-function-handler
                    (make-post-request {}))]
      (is (= 400 (:status response))))))

;; ---- gap: content-type header on GET responses ----

(deftest get-functions-response-has-json-content-type
  (testing "GET /functions sets application/json content-type header"
    (let [response (fn-handler/get-functions-handler {})]
      (is (= "application/json" (get-in response [:headers "Content-Type"]))))))

(deftest get-function-by-id-response-has-json-content-type
  (testing "GET /functions/:id sets application/json content-type header"
    (let [func (db/add-function {:name "ct-test" :description "content type"
                                 :language "assemblyscript"
                                 :source "source" :status "READY"})
          fn-id (:functions/id func)
          response (fn-handler/get-function-by-id-handler
                    (make-get-request {:id (str fn-id)}))]
      (is (= "application/json" (get-in response [:headers "Content-Type"]))))))
