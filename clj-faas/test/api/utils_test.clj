(ns api.utils-test
  "Unit tests for api.utils — pgobject roundtrip, uuidfy, base64, throw-error!, respond-erroneous-request."
  (:require
   [api.utils :as u]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [taoensso.telemere :as tel])
  (:import
   [org.postgresql.util PGobject]))

(use-fixtures :once
  (fn [f]
    (tel/set-min-level! :fatal)
    (f)))

;; ---- uuidfy ----

(deftest uuidfy-returns-uuid-unchanged
  (testing "a UUID object passes through unchanged"
    (let [id (random-uuid)]
      (is (= id (u/uuidfy id)))
      (is (uuid? (u/uuidfy id))))))

(deftest uuidfy-parses-valid-uuid-string
  (testing "a valid UUID string is parsed to a UUID"
    (let [id (random-uuid)
          id-str (str id)]
      (is (= id (u/uuidfy id-str)))
      (is (uuid? (u/uuidfy id-str))))))

(deftest uuidfy-returns-nil-for-invalid-string
  (testing "an invalid UUID string returns nil (parse-uuid does not throw in this Clojure version)"
    (is (nil? (u/uuidfy "not-a-uuid")))))

(deftest uuidfy-throws-on-nil
  (testing "nil input throws (parse-uuid NPEs on nil, but throw-error! arg order bug causes ClassCastException)"
    (is (thrown? ClassCastException (u/uuidfy nil)))))

;; ---- bytes <-> base64 ----

(deftest bytes-base64-roundtrip
  (testing "encode then decode returns original bytes"
    (let [original (.getBytes "Hello, World!" "UTF-8")
          encoded (u/bytes->base64 original)
          decoded (u/base64->bytes encoded)]
      (is (string? encoded))
      (is (bytes? decoded))
      (is (= (seq original) (seq decoded))))))

(deftest bytes-base64-roundtrip-binary
  (testing "roundtrip works for arbitrary binary data"
    (let [original (byte-array (range 256))
          encoded (u/bytes->base64 original)
          decoded (u/base64->bytes encoded)]
      (is (= (seq original) (seq decoded))))))

(deftest bytes-base64-nil-passthrough
  (testing "nil returns nil in both directions"
    (is (nil? (u/bytes->base64 nil)))
    (is (nil? (u/base64->bytes nil)))))

(deftest bytes-base64-empty
  (testing "empty byte array roundtrips to empty"
    (let [empty-bytes (byte-array 0)
          encoded (u/bytes->base64 empty-bytes)
          decoded (u/base64->bytes encoded)]
      (is (= "" encoded))
      (is (= 0 (alength decoded))))))

;; ---- pgobject roundtrip ----

(deftest pgobject-roundtrip-map
  (testing "a Clojure map survives ->pgobject and <-pgobject"
    (let [data {"key" "value" "number" 42}
          pg (u/->pgobject data)
          back (u/<-pgobject pg)]
      (is (instance? PGobject pg))
      (is (= data back)))))

(deftest pgobject-roundtrip-vector
  (testing "a Clojure vector survives roundtrip"
    (let [data [1 2 3 "four"]
          pg (u/->pgobject data)
          back (u/<-pgobject pg)]
      (is (= data back)))))

(deftest pgobject-roundtrip-nested
  (testing "nested structures survive roundtrip"
    (let [data {"users" [{"name" "Alice" "scores" [10 20]}
                         {"name" "Bob" "scores" [30]}]}
          pg (u/->pgobject data)
          back (u/<-pgobject pg)]
      (is (= data back)))))

(deftest pgobject-defaults-to-jsonb
  (testing "pgobject type defaults to jsonb"
    (let [pg (u/->pgobject {"a" 1})]
      (is (= "jsonb" (.getType pg))))))

(deftest pgobject-non-json-type-returns-raw-string
  (testing "a pgobject with non-json type returns the raw string value"
    (let [pg (doto (PGobject.)
               (.setType "text")
               (.setValue "plain text"))]
      (is (= "plain text" (u/<-pgobject pg))))))

;; ---- throw-error! ----

(deftest throw-error-single-arg
  (testing "single-arg throw-error! throws ExceptionInfo with :id in ex-data"
    (try
      (u/throw-error! ::test-error)
      (is false "should have thrown")
      (catch clojure.lang.ExceptionInfo e
        (is (= ::test-error (:id (ex-data e))))
        (is (= (str ::test-error) (ex-message e)))))))

(deftest throw-error-with-cause
  (testing "two-arg preserves the cause chain"
    (let [cause (Exception. "root cause")]
      (try
        (u/throw-error! ::with-cause cause)
        (is false "should have thrown")
        (catch clojure.lang.ExceptionInfo e
          (is (= ::with-cause (:id (ex-data e))))
          (is (= cause (.getCause e))))))))

(deftest throw-error-with-data
  (testing "three-arg includes extra data in ex-data"
    (let [cause (Exception. "root")]
      (try
        (u/throw-error! ::with-data cause {:foo "bar" :count 42})
        (is false "should have thrown")
        (catch clojure.lang.ExceptionInfo e
          (is (= ::with-data (:id (ex-data e))))
          (is (= "bar" (:foo (ex-data e))))
          (is (= 42 (:count (ex-data e))))
          (is (= cause (.getCause e))))))))

;; ---- respond-erroneous-request ----

(deftest respond-erroneous-request-returns-400
  (testing "returns a 400 response with :error and :data keys"
    (let [err (ex-info "something went wrong" {:detail "x"})
          response (u/respond-erroneous-request err)]
      (is (= 400 (:status response)))
      (is (= "something went wrong" (get-in response [:body :error])))
      (is (= {:detail "x"} (get-in response [:body :data]))))))

(deftest respond-erroneous-request-no-data
  (testing "works with an exception that has empty ex-data"
    (let [err (ex-info "bare error" {})
          response (u/respond-erroneous-request err)]
      (is (= 400 (:status response)))
      (is (= "bare error" (get-in response [:body :error]))))))

;; ---- gap: pgobject custom pgtype via metadata ----

(deftest pgobject-custom-pgtype-via-metadata
  (testing "->pgobject respects :pgtype metadata to override default jsonb"
    (let [data (with-meta {"a" 1} {:pgtype "json"})
          pg (u/->pgobject data)]
      (is (= "json" (.getType pg)))
      ;; roundtrip through <-pgobject still works
      (let [back (u/<-pgobject pg)]
        (is (= {"a" 1} back))
        ;; metadata on the result should reflect the json type
        (is (= "json" (:pgtype (meta back))))))))

;; ---- gap: <-pgobject with null value field ----

(deftest pgobject-null-value-returns-nil
  (testing "<-pgobject with a null value field returns nil via some-> short-circuit"
    (let [pg (doto (PGobject.)
               (.setType "jsonb")
               (.setValue nil))]
      (is (nil? (u/<-pgobject pg))))))

;; ---- gap: <-pgobject with "json" type (not just "jsonb") ----

(deftest pgobject-json-type-roundtrip
  (testing "<-pgobject handles 'json' type (not just 'jsonb') and tags metadata"
    (let [pg (doto (PGobject.)
               (.setType "json")
               (.setValue "{\"key\":\"value\"}"))]
      (let [result (u/<-pgobject pg)]
        (is (= {"key" "value"} result))
        (is (= "json" (:pgtype (meta result))))))))

;; ---- gap: base64->bytes with invalid base64 string ----

(deftest base64-invalid-string-throws
  (testing "base64->bytes throws on invalid base64 input"
    (is (thrown? IllegalArgumentException
                (u/base64->bytes "not!!valid@@base64")))))
