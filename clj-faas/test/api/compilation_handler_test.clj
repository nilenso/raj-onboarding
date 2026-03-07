(ns api.compilation-handler-test
  "Integration tests for the compilation results handler.
   Tests the process-compilation-result flow that the poller drives:
   pgmq message → sanitize → update function status → delete message."
  (:require
   [api.db :as db]
   [api.handlers.compilations :as comp-handler]
   [api.pgmq :as pgmq]
   [api.utils :as u]
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
    (pgmq/purge-queues)
    (f)))

(defn- create-compiling-function
  "Insert a function with COMPILING status and return it."
  [name]
  (db/add-function {:name name
                    :description (str name " test function")
                    :language "assemblyscript"
                    :source "export function handle(input: string): string { return input; }"
                    :status "COMPILING"}))

(defn- make-success-result
  "Build a successful compilation result matching the compiler's schema."
  [fn-id wasm-bytes]
  {:functionId (str fn-id)
   :success true
   :wasmBinary (u/bytes->base64 wasm-bytes)
   :error nil})

(defn- make-failure-result
  "Build a failed compilation result."
  [fn-id error-msg]
  {:functionId (str fn-id)
   :success false
   :wasmBinary nil
   :error error-msg})

(defn- publish-and-read-result
  "Publish a compilation result to pgmq and read it back (adding :msg-id)."
  [result-map]
  (pgmq/publish-message "compilation_results" result-map)
  (pgmq/peek-result))

;; ---- successful compilation ----

(deftest process-successful-compilation-makes-function-ready
  (testing "a successful compilation result transitions the function from COMPILING to READY"
    (let [func (create-compiling-function "compile-success")
          fn-id (:functions/id func)
          wasm-bytes (.getBytes "fake wasm binary" "UTF-8")
          result (publish-and-read-result (make-success-result fn-id wasm-bytes))]
      (comp-handler/process result)
      (let [updated (db/get-function-by-id fn-id)]
        (is (= "READY" (:functions/status updated)))))))

(deftest process-successful-compilation-stores-wasm-binary
  (testing "the wasm binary is base64-decoded and stored in the function record"
    (let [func (create-compiling-function "wasm-stored")
          fn-id (:functions/id func)
          wasm-bytes (.getBytes "wasm module content" "UTF-8")
          result (publish-and-read-result (make-success-result fn-id wasm-bytes))]
      (comp-handler/process result)
      (let [updated (db/get-function-by-id fn-id)]
        (is (some? (:functions/wasm_binary updated)))
        (is (= (seq wasm-bytes) (seq (:functions/wasm_binary updated))))))))

(deftest process-successful-compilation-clears-compile-error
  (testing "a successful result stores nil compile_error"
    (let [func (create-compiling-function "no-error")
          fn-id (:functions/id func)
          result (publish-and-read-result
                  (make-success-result fn-id (.getBytes "wasm" "UTF-8")))]
      (comp-handler/process result)
      (let [updated (db/get-function-by-id fn-id)]
        (is (nil? (:functions/compile_error updated)))))))

;; ---- failed compilation ----

(deftest process-failed-compilation-makes-function-failed
  (testing "a failed compilation result transitions the function from COMPILING to FAILED"
    (let [func (create-compiling-function "compile-fail")
          fn-id (:functions/id func)
          result (publish-and-read-result
                  (make-failure-result fn-id "Type error on line 42"))]
      (comp-handler/process result)
      (let [updated (db/get-function-by-id fn-id)]
        (is (= "FAILED" (:functions/status updated)))))))

(deftest process-failed-compilation-stores-compile-error
  (testing "the error message from the compiler is stored in the function record"
    (let [func (create-compiling-function "error-stored")
          fn-id (:functions/id func)
          error-msg "Cannot find module 'foo'"
          result (publish-and-read-result (make-failure-result fn-id error-msg))]
      (comp-handler/process result)
      (let [updated (db/get-function-by-id fn-id)]
        (is (= error-msg (:functions/compile_error updated)))))))

;; ---- idempotency ----

(deftest process-compilation-idempotent-on-ready
  (testing "processing a result for an already-READY function is a no-op"
    (let [func (db/add-function {:name "already-ready"
                                 :description "already compiled"
                                 :language "assemblyscript"
                                 :source "source"
                                 :status "READY"})
          fn-id (:functions/id func)
          result (publish-and-read-result
                  (make-success-result fn-id (.getBytes "new wasm" "UTF-8")))]
      (comp-handler/process result)
      (let [after (db/get-function-by-id fn-id)]
        ;; status unchanged
        (is (= "READY" (:functions/status after)))
        ;; wasm_binary not overwritten (was nil, stays nil since update was skipped)
        (is (nil? (:functions/wasm_binary after)))))))

(deftest process-compilation-idempotent-on-failed
  (testing "processing a result for an already-FAILED function is a no-op"
    (let [func (db/add-function {:name "already-failed"
                                 :description "already failed"
                                 :language "assemblyscript"
                                 :source "source"
                                 :status "FAILED"})
          fn-id (:functions/id func)
          result (publish-and-read-result
                  (make-success-result fn-id (.getBytes "retry wasm" "UTF-8")))]
      (comp-handler/process result)
      (let [after (db/get-function-by-id fn-id)]
        (is (= "FAILED" (:functions/status after)))))))

;; ---- pgmq message cleanup ----

(deftest process-compilation-deletes-pgmq-message
  (testing "the pgmq message is deleted after processing, preventing re-processing"
    (let [func (create-compiling-function "msg-cleanup")
          fn-id (:functions/id func)
          result (publish-and-read-result
                  (make-success-result fn-id (.getBytes "wasm" "UTF-8")))]
      (comp-handler/process result)
      ;; queue should be empty after processing
      (is (nil? (pgmq/peek-result))))))

(deftest process-compilation-deletes-message-even-when-idempotent
  (testing "pgmq message is deleted even when the update is skipped (terminal state)"
    (let [func (db/add-function {:name "terminal-cleanup"
                                 :description "already done"
                                 :language "assemblyscript"
                                 :source "source"
                                 :status "READY"})
          fn-id (:functions/id func)
          result (publish-and-read-result
                  (make-success-result fn-id (.getBytes "wasm" "UTF-8")))]
      (comp-handler/process result)
      (is (nil? (pgmq/peek-result))))))

;; ---- gap: PENDING function is not terminal → update is applied ----

(deftest process-compilation-on-pending-function-applies-update
  (testing "a PENDING function (not terminal) receives the compilation update normally"
    (let [func (db/add-function {:name "pending-fn"
                                 :description "still pending"
                                 :language "assemblyscript"
                                 :source "export function handle(input: string): string { return input; }"
                                 :status "PENDING"})
          fn-id (:functions/id func)
          wasm-bytes (.getBytes "pending wasm" "UTF-8")
          result (publish-and-read-result (make-success-result fn-id wasm-bytes))]
      (comp-handler/process result)
      (let [updated (db/get-function-by-id fn-id)]
        (is (= "READY" (:functions/status updated)))
        (is (some? (:functions/wasm_binary updated)))
        (is (= (seq wasm-bytes) (seq (:functions/wasm_binary updated))))))))
