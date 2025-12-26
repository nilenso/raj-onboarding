package com.projectnil.api.runtime;

/**
 * Exception thrown when WASM execution fails at runtime.
 * 
 * <p>This includes:
 * <ul>
 *   <li>WASM traps (unreachable, divide by zero, etc.)</li>
 *   <li>Execution timeouts</li>
 *   <li>Invalid output from the module</li>
 * </ul>
 */
public class WasmExecutionException extends RuntimeException {

    public WasmExecutionException(String message) {
        super(message);
    }

    public WasmExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
