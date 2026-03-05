package com.projectnil.api.runtime;

/**
 * Exception thrown when a WASM module violates the expected ABI contract.
 * 
 * <p>This includes:
 * <ul>
 *   <li>Missing required exports (e.g., 'handle' function)</li>
 *   <li>Missing memory management exports (e.g., '__new', '__pin', '__unpin')</li>
 *   <li>Invalid export signatures</li>
 * </ul>
 */
public class WasmAbiException extends RuntimeException {

    public WasmAbiException(String message) {
        super(message);
    }

    public WasmAbiException(String message, Throwable cause) {
        super(message, cause);
    }
}
