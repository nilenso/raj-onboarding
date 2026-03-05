package com.projectnil.api.runtime;

public interface WasmRuntime {
    /**
     * Executes WASM binary with the provided JSON input.
     * @param wasmBinary the compiled WASM module
     * @param inputJson the input parameters as a JSON string
     * @return JSON output as bytes
     * @throws Exception if execution fails
     */
    byte[] execute(byte[] wasmBinary, String inputJson) throws Exception;
}
