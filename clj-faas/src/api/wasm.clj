(ns api.wasm
  (:import
   [com.projectnil.api.runtime ChicoryWasmRuntime AssemblyScriptStringCodec]
   [java.time Duration]
   [java.nio.charset StandardCharsets]))

(defn make-runtime
  "Create a ChicoryWasmRuntime with the given timeout in milliseconds."
  [timeout-ms]
  (ChicoryWasmRuntime. (AssemblyScriptStringCodec.)
                       (Duration/ofMillis timeout-ms)))

(defn execute
  "Execute a WASM binary with the given input JSON string.
   Returns the output as a string."
  [runtime wasm-bytes input-json]
  (String. (.execute runtime wasm-bytes input-json)
           StandardCharsets/UTF_8))
