(ns api.pgmq-test
  (:require
   [api.db :as api-db]
   [api.pgmq :as api-pgmq]
   [api.utils :as u]
   [clojure.test :as t :refer [deftest is use-fixtures]]))

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
  (let [fuuid (random-uuid)
        job {:functions/id fuuid
             :functions/language "clojure"
             :functions/source "(println \"Hello, World!\")"}]
    (is (= [{:pgmq.send fuuid}] (api-pgmq/publish-pgmq-job job)))))

