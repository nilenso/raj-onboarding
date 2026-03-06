package com.projectnil.api.runtime;

import com.dylibso.chicory.runtime.Instance;

/**
 * Abstraction for language-specific string memory handling in WASM modules.
 * 
 * <p>Different source languages (AssemblyScript, Rust, Go) have different memory
 * models and string encodings. This interface allows the runtime to support
 * multiple languages by swapping codec implementations.
 * 
 * <p>Phase 0 supports AssemblyScript via {@link AssemblyScriptStringCodec}.
 */
public interface WasmStringCodec {

    /**
     * Validates that the WASM module exports all required functions for this codec.
     * 
     * @param instance the instantiated WASM module
     * @throws WasmAbiException if required exports are missing
     */
    void validateExports(Instance instance) throws WasmAbiException;

    /**
     * Writes a Java string into WASM linear memory.
     * 
     * <p>The implementation is responsible for:
     * <ul>
     *   <li>Allocating memory in the module</li>
     *   <li>Encoding the string appropriately (e.g., UTF-16LE for AssemblyScript)</li>
     *   <li>Pinning memory if required by the language's GC</li>
     * </ul>
     * 
     * @param instance the instantiated WASM module
     * @param value the Java string to write
     * @return pointer to the string in WASM linear memory
     */
    int writeString(Instance instance, String value);

    /**
     * Reads a string from WASM linear memory at the given pointer.
     * 
     * @param instance the instantiated WASM module
     * @param pointer pointer to the string in WASM memory
     * @return the decoded Java string
     */
    String readString(Instance instance, int pointer);

    /**
     * Cleanup any pinned or allocated memory after execution.
     * 
     * <p>Called in a finally block to ensure memory is released even on errors.
     * 
     * @param instance the instantiated WASM module
     * @param pointer pointer to the memory to cleanup
     */
    void cleanup(Instance instance, int pointer);
}
