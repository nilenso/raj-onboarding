(ns api.db-execution-test
  (:require
   [api.db :as db]
   [api.utils :as u]
   [test-helpers :as h]
   [taoensso.telemere :as tel]
   [clojure.test :refer [deftest is testing use-fixtures]]))

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

(defn- create-test-function
  "Helper: insert a function and return it"
  ([] (create-test-function {}))
  ([overrides]
   (db/add-function (merge {:name "test-fn"
                            :description "test function"
                            :language "assemblyscript"
                            :source "export function handle(input: string): string { return input; }"
                            :status "READY"}
                           overrides))))

;; add-execution tests

(deftest add-execution-creates-record
  (testing "add-execution inserts a record and returns it with an id"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          execution (db/add-execution {:function_id fn-id
                                       :input {:message "hello"}
                                       :status "RUNNING"
                                       :started_at (java.time.LocalDateTime/now)})]
      (is (some? (:executions/id execution)))
      (is (= fn-id (:executions/function_id execution)))
      (is (= "RUNNING" (:executions/status execution))))))

(deftest add-execution-stores-input-as-jsonb
  (testing "input map is stored and retrievable as JSONB"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          input-data {:key "value" :nested {:a 1 :b [1 2 3]}}
          execution (db/add-execution {:function_id fn-id
                                       :input input-data
                                       :status "PENDING"})
          ex-id (:executions/id execution)
          fetched (db/get-execution-by-id ex-id)]
      (is (= {"key" "value" "nested" {"a" 1 "b" [1 2 3]}}
             (:input fetched))))))

(deftest add-execution-with-nil-input
  (testing "execution can be created with nil input"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          execution (db/add-execution {:function_id fn-id
                                       :status "PENDING"})]
      (is (some? (:executions/id execution))))))

;; update-execution tests

(deftest update-execution-status
  (testing "update-execution changes status and returns updated record"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          execution (db/add-execution {:function_id fn-id
                                       :input {:msg "test"}
                                       :status "RUNNING"})
          ex-id (:executions/id execution)
          updated (db/update-execution ex-id {:status "COMPLETED"
                                              :completed_at (java.time.LocalDateTime/now)})]
      (is (= "COMPLETED" (:status updated))))))

(deftest update-execution-with-output
  (testing "update-execution stores output as JSONB"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          execution (db/add-execution {:function_id fn-id
                                       :input {:a 1}
                                       :status "RUNNING"})
          ex-id (:executions/id execution)
          output-data {"result" "success" "sum" 42}
          updated (db/update-execution ex-id {:status "COMPLETED"
                                              :output output-data})]
      (is (= "COMPLETED" (:status updated)))
      (is (= output-data (:output updated))))))

(deftest update-execution-with-error
  (testing "update-execution stores error message on failure"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          execution (db/add-execution {:function_id fn-id
                                       :input {}
                                       :status "RUNNING"})
          ex-id (:executions/id execution)
          updated (db/update-execution ex-id {:status "FAILED"
                                              :error_message "WASM trap: unreachable instruction"})]
      (is (= "FAILED" (:status updated)))
      (is (= "WASM trap: unreachable instruction" (:error_message updated))))))

;; get-execution-by-id tests

(deftest get-execution-by-id-returns-deserialized-json
  (testing "get-execution-by-id deserializes input/output from JSONB"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          execution (db/add-execution {:function_id fn-id
                                       :input {:msg "hello"}
                                       :status "RUNNING"})
          ex-id (:executions/id execution)
          _ (db/update-execution ex-id {:status "COMPLETED"
                                        :output {"greeting" "Hello!"}})
          fetched (db/get-execution-by-id ex-id)]
      (is (= {"msg" "hello"} (:input fetched)))
      (is (= {"greeting" "Hello!"} (:output fetched))))))

(deftest get-execution-by-id-nil-output
  (testing "get-execution-by-id handles nil output gracefully"
    (let [func (create-test-function)
          fn-id (:functions/id func)
          execution (db/add-execution {:function_id fn-id
                                       :input {:a 1}
                                       :status "RUNNING"})
          ex-id (:executions/id execution)
          fetched (db/get-execution-by-id ex-id)]
      (is (= {"a" 1} (:input fetched)))
      (is (nil? (:output fetched))))))

;; get-function-executions tests

(deftest get-function-executions-filters-by-function
  (testing "get-function-executions returns only executions for the given function"
    (let [func-a (create-test-function {:name "func-a"})
          func-b (create-test-function {:name "func-b"})
          fn-a-id (:functions/id func-a)
          fn-b-id (:functions/id func-b)]
      (db/add-execution {:function_id fn-a-id :input {:x 1} :status "COMPLETED"})
      (db/add-execution {:function_id fn-a-id :input {:x 2} :status "COMPLETED"})
      (db/add-execution {:function_id fn-b-id :input {:x 3} :status "COMPLETED"})
      (let [a-execs (db/get-function-executions fn-a-id)
            b-execs (db/get-function-executions fn-b-id)]
        (is (= 2 (count a-execs)))
        (is (= 1 (count b-execs)))))))

;; get-executions tests

(deftest get-executions-returns-all
  (testing "get-executions returns all executions across functions"
    (let [func-a (create-test-function {:name "func-a"})
          func-b (create-test-function {:name "func-b"})
          fn-a-id (:functions/id func-a)
          fn-b-id (:functions/id func-b)]
      (db/add-execution {:function_id fn-a-id :input {:x 1} :status "RUNNING"})
      (db/add-execution {:function_id fn-b-id :input {:x 2} :status "RUNNING"})
      (let [all-execs (db/get-executions)]
        (is (= 2 (count all-execs)))))))

(deftest get-executions-empty-table
  (testing "get-executions returns empty vec when no executions exist"
    (let [all-execs (db/get-executions)]
      (is (= [] all-execs)))))

;; cascade delete tests

(deftest delete-function-cascades-to-executions
  (testing "deleting a function cascades to its executions"
    (let [func (create-test-function)
          fn-id (:functions/id func)]
      (db/add-execution {:function_id fn-id :input {:x 1} :status "COMPLETED"})
      (db/add-execution {:function_id fn-id :input {:x 2} :status "FAILED"})
      (is (= 2 (count (db/get-function-executions fn-id))))
      (db/delete-function fn-id)
      (is (= 0 (count (db/get-executions)))))))

;; ---- gap: get-execution-by-id with non-existent UUID ----

(deftest get-execution-by-id-nonexistent-returns-empty-map
  (testing "get-execution-by-id returns {} for a UUID that does not exist (sanitize-execution converts nil to empty map)"
    (let [result (db/get-execution-by-id (random-uuid))]
      (is (= {} result)))))

;; ---- gap: get-function-executions with 0 executions ----

(deftest get-function-executions-empty-returns-empty-vec
  (testing "get-function-executions returns [] when a function has no executions"
    (let [func (create-test-function {:name "lonely-fn"})
          fn-id (:functions/id func)
          result (db/get-function-executions fn-id)]
      (is (= [] result))
      (is (vector? result)))))
