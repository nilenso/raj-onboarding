(ns api.wasm-test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.java.io :as io]
   [api.wasm :as wasm])
  (:import
   [com.projectnil.api.runtime WasmExecutionException WasmAbiException]))

(def default-timeout-ms 10000)
(def short-timeout-ms 1000)

(defn- load-wasm
  "Load a .wasm file from test resources by name."
  [name]
  (let [path (str "wasm/" name ".wasm")
        resource (io/resource path)]
    (when-not resource
      (throw (ex-info (str "WASM resource not found: " path) {:path path})))
    (with-open [is (.openStream resource)]
      (.readAllBytes is))))

(def runtime (wasm/make-runtime default-timeout-ms))

;; success scenarios

(deftest echo-returns-input-unchanged
  (let [wasm-binary (load-wasm "echo")
        input "{\"foo\":\"bar\",\"num\":42}"
        result (wasm/execute runtime wasm-binary input)]
    (is (= input result))))

(deftest echo-handles-empty-object
  (let [wasm-binary (load-wasm "echo")
        result (wasm/execute runtime wasm-binary "{}")]
    (is (= "{}" result))))

(deftest echo-handles-unicode
  (let [wasm-binary (load-wasm "echo")
        input "{\"message\":\"Hello, 世界! 🌍\"}"
        result (wasm/execute runtime wasm-binary input)]
    (is (= input result))))

(deftest add-computes-sum
  (let [wasm-binary (load-wasm "add")
        result (wasm/execute runtime wasm-binary "{\"a\":10,\"b\":5}")]
    (is (= "{\"sum\":15}" result))))

(deftest add-handles-negatives
  (let [wasm-binary (load-wasm "add")
        result (wasm/execute runtime wasm-binary "{\"a\":-5,\"b\":3}")]
    (is (= "{\"sum\":-2}" result))))

(deftest greet-concatenates-string
  (let [wasm-binary (load-wasm "greet")
        result (wasm/execute runtime wasm-binary "{\"name\":\"Alice\"}")]
    (is (= "{\"greeting\":\"Hello, Alice!\"}" result))))

(deftest greet-uses-default-name
  (let [wasm-binary (load-wasm "greet")
        result (wasm/execute runtime wasm-binary "{}")]
    (is (= "{\"greeting\":\"Hello, World!\"}" result))))

;; abi validation

(deftest missing-handle-export-throws
  (let [wasm-binary (load-wasm "no-handle")]
    (is (thrown? WasmAbiException
                (wasm/execute runtime wasm-binary "{}")))))

(deftest invalid-binary-throws
  (is (thrown? WasmExecutionException
              (wasm/execute runtime (.getBytes "not a wasm module") "{}"))))

(deftest empty-binary-throws
  (is (thrown? WasmExecutionException
              (wasm/execute runtime (byte-array 0) "{}"))))

;; runtime errors

(deftest trap-throws-execution-exception
  (let [wasm-binary (load-wasm "trap")]
    (is (thrown? WasmExecutionException
                (wasm/execute runtime wasm-binary "{}")))))

(deftest infinite-loop-times-out
  (let [wasm-binary (load-wasm "infinite-loop")
        short-runtime (wasm/make-runtime short-timeout-ms)]
    (is (thrown? WasmExecutionException
                (wasm/execute short-runtime wasm-binary "{}")))))
