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
    (api-pgmq/purge-pgmq-queues)
    (f)))

(deftest publish-pgmq-job-test
  (testing "publishing a job to pgmq should return a message id without an error"
    (let [fuuid (random-uuid)
          job {:functions/id fuuid
               :functions/language "clojure"
               :functions/source "(println \"Hello, World!\")"}]
      (is (< 0 (:send (api-pgmq/publish-pgmq-job job)))))))

(deftest publish-pgmq-job-validation-test
  (testing "publishing a job with missing keys should throw an assertion error"
    (is (thrown? AssertionError
                 (api-pgmq/publish-pgmq-job {:functions/id (random-uuid)
                                             :functions/language "clojure"})))))

(deftest publish-pgmq-job-and-read-back-test
  (testing "publishing a job and then reading it back should return the same job"
    (let [fuuid (random-uuid)
          job {:functions/id fuuid
               :functions/language "clojure"
               :functions/source "(println \"Hello, World!\")"}]
      (api-pgmq/publish-pgmq-job job)
      (is (= (str (:functions/id job)) (:id (api-pgmq/read-one-from-pgmq "compilation_jobs")))))))

(deftest read-pgmq-empty-queue-result-test
  (testing "reading from an empty pgmq queue should return nil"
    (api-pgmq/purge-pgmq-queues)
    (is (= nil (api-pgmq/read-pgmq-result)))))

(deftest simulate-published-result-read-test
  (testing "reading from pgmq should return the published message"
    (let [fuuid (random-uuid)
          job {:id fuuid
               :language "clojure"
               :source "(println \"Hello, World!\")"
               :status "success"
               :wasm-bin "00"}]
      (api-pgmq/publish-pgmq-message "compilation_results" job)
      (is (= (str (:id job)) (:id (api-pgmq/read-pgmq-result)))))))

(deftest simulate-invalid-published-result-read-test
  (testing "receiving incomplete result from pgmq should throw an assertion error"
    (let [fuuid (random-uuid)
          result {:id fuuid
                  :language "clojure"
                  :source "(println \"Hello, World!\")"}]
      (api-pgmq/publish-pgmq-message "compilation_results" result)
      (is (h/thrown-with-id? :api.pgmq/pgmq-result-missing-keys #(api-pgmq/read-pgmq-result))))))

(deftest delete-existing-pgmq-msg-test
  (testing "deleting a message from pgmq should return true and remove the message from the queue"
    (let [fuuid (random-uuid)
          job {:functions/id fuuid
               :functions/language "clojure"
               :functions/source "(println \"Hello, World!\")"}
          msg-id (:send (api-pgmq/publish-pgmq-job job))]
      (let [msg (api-pgmq/read-one-from-pgmq "compilation_jobs")]
        (is (= (str (:functions/id job)) (:id msg)))
        (is (= {:delete true} (api-pgmq/delete-pgmq-msg "compilation_jobs" msg-id)))))))

(deftest delete-non-existent-pgmq-msg-test
  (testing "deleting a non-existent message from pgmq should return false without an error"
    (is (= {:delete false} (api-pgmq/delete-pgmq-msg "compilation_jobs" 10000000)))))
