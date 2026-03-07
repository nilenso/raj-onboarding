(ns api.pgmq-test
  (:require
   [api.db :as api-db]
   [api.pgmq :as api-pgmq]
   [api.utils :as u]
   [test-helpers :as h]
   [clojure.test :as t :refer [deftest is use-fixtures testing]]))

(use-fixtures :once
  (fn [f]
    ;; (tel/set-min-level! :fatal)
    (u/process-cli-args ["-c" "config.edn"
                         "-s" "secrets-test.edn"])
    (api-db/start-pool!)
    (try (f)
         (finally (api-db/stop-pool!)))))

(use-fixtures :each
  (fn [f]
    (api-pgmq/purge-queues)
    (f)))

(deftest publish-compilation-job-test
  (testing "publishing a job to pgmq should return a message id without an error"
    (let [fuuid (random-uuid)
          job {:functions/id fuuid
               :functions/language "clojure"
               :functions/source "(println \"Hello, World!\")"}]
      (is (< 0 (:send (api-pgmq/publish-compilation-job job)))))))

(deftest publish-compilation-job-missing-keys-test
  (testing "publishing a job with missing source key succeeds with nil source (no precondition in code)"
    (let [result (api-pgmq/publish-compilation-job {:functions/id (random-uuid)
                                                    :functions/language "clojure"})]
      (is (< 0 (:send result))))))

(deftest publish-compilation-job-and-read-back-test
  (testing "publishing a job and then reading it back should return the same job"
    (let [fuuid (random-uuid)
          job {:functions/id fuuid
               :functions/language "clojure"
               :functions/source "(println \"Hello, World!\")"}]
      (api-pgmq/publish-compilation-job job)
      (is (= (str (:functions/id job)) (:functionId (first (api-pgmq/peek-queue "compilation_jobs"))))))))

(deftest read-pgmq-empty-queue-result-test
  (testing "reading from an empty pgmq queue should return nil"
    (api-pgmq/purge-queues)
    (is (= nil (api-pgmq/peek-result)))))

(deftest simulate-published-result-read-test
  (testing "reading from pgmq should return the published message"
    (let [fuuid (random-uuid)
          job {:functionId fuuid
               :success true
               :wasmBinary "00"
               :error nil}]
      (api-pgmq/publish-message "compilation_results" job)
      (is (= (str (:functionId job)) (:functionId (api-pgmq/peek-result)))))))

(deftest simulate-invalid-published-result-read-test
  (testing "receiving incomplete result from pgmq should throw an assertion error"
    (let [fuuid (random-uuid)
          result {:id fuuid
                  :language "clojure"
                  :source "(println \"Hello, World!\")"}]
      (api-pgmq/publish-message "compilation_results" result)
      (is (h/thrown-with-id? :api.pgmq/pgmq-result-missing-keys #(api-pgmq/peek-result))))))

(deftest delete-existing-pgmq-msg-test
  (testing "deleting a message from pgmq should return true and remove the message from the queue"
    (let [fuuid (random-uuid)
          job {:functions/id fuuid
               :functions/language "clojure"
               :functions/source "(println \"Hello, World!\")"}
          msg-id (:send (api-pgmq/publish-compilation-job job))
          msg (first (api-pgmq/peek-queue "compilation_jobs"))]
      (is (= (str (:functions/id job)) (:functionId msg)))
      (is (= {:delete true} (api-pgmq/delete-message "compilation_jobs" msg-id))))))

(deftest delete-non-existent-pgmq-msg-test
  (testing "deleting a non-existent message from pgmq should return false without an error"
    (is (= {:delete false} (api-pgmq/delete-message "compilation_jobs" 10000000)))))

;; ---- batch reading ----

(deftest peek-results-batch-returns-multiple
  (testing "batch read returns all published valid results"
    (dotimes [_i 3]
      (api-pgmq/publish-message "compilation_results"
                                {:functionId (random-uuid)
                                 :success true
                                 :wasmBinary "AA=="
                                 :error nil}))
    (let [results (api-pgmq/peek-results-batch 5 0)]
      (is (= 3 (count results)))
      (is (every? :functionId results))
      (is (every? :msg-id results)))))

(deftest peek-results-batch-empty-queue
  (testing "batch read on empty queue returns empty vector"
    (let [results (api-pgmq/peek-results-batch 5 0)]
      (is (= [] results)))))

(deftest peek-results-batch-rejects-invalid
  (testing "batch read with invalid results throws"
    (api-pgmq/publish-message "compilation_results"
                              {:functionId (random-uuid)
                               :success true
                               :wasmBinary "AA=="
                               :error nil})
    (api-pgmq/publish-message "compilation_results"
                              {:bad-key "invalid schema"})
    (is (h/thrown-with-id? :api.pgmq/pgmq-result-missing-keys
                           #(api-pgmq/peek-results-batch 5 0)))))
