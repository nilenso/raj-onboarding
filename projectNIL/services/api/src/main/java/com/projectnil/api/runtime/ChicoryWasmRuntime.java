package com.projectnil.api.runtime;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WASM runtime implementation using Chicory (pure Java WebAssembly runtime).
 * 
 * <p>This implementation:
 * <ul>
 *   <li>Parses WASM binary and instantiates the module</li>
 *   <li>Validates the required 'handle' export exists</li>
 *   <li>Uses {@link WasmStringCodec} for language-specific string I/O</li>
 *   <li>Enforces configurable execution timeout</li>
 *   <li>Logs warnings for large memory usage</li>
 * </ul>
 * 
 * @see <a href="https://chicory.dev/docs/">Chicory Documentation</a>
 */
public class ChicoryWasmRuntime implements WasmRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChicoryWasmRuntime.class);

    /**
     * Memory warning threshold: 16MB (256 pages * 64KB per page).
     */
    private static final int MEMORY_WARNING_PAGES = 256;

    private final WasmStringCodec stringCodec;
    private final Duration timeout;

    public ChicoryWasmRuntime(WasmStringCodec stringCodec, Duration timeout) {
        this.stringCodec = stringCodec;
        this.timeout = timeout;
    }

    @Override
    public byte[] execute(byte[] wasmBinary, String inputJson) throws WasmExecutionException {
        LOGGER.debug("Executing WASM module ({} bytes) with input: {}", 
            wasmBinary.length, truncateForLog(inputJson));

        // 1. Parse WASM binary
        WasmModule module = parseModule(wasmBinary);

        // 2. Instantiate module
        Instance instance = instantiateModule(module);

        // 3. Check memory usage and log warning if high
        checkMemoryUsage(instance);

        // 4. Validate ABI
        validateHandleExport(instance);
        stringCodec.validateExports(instance);

        // 5. Write input to WASM memory
        int inputPtr = stringCodec.writeString(instance, inputJson);

        try {
            // 6. Execute with timeout
            ExportFunction handle = instance.export("handle");
            int outputPtr = executeWithTimeout(handle, inputPtr);

            // 7. Read output
            String output = stringCodec.readString(instance, outputPtr);
            if (output == null) {
                throw new WasmExecutionException("WASM function returned null");
            }

            LOGGER.debug("WASM execution completed, output: {}", truncateForLog(output));
            return output.getBytes(StandardCharsets.UTF_8);

        } finally {
            // 8. Cleanup pinned memory
            stringCodec.cleanup(instance, inputPtr);
        }
    }

    private WasmModule parseModule(byte[] wasmBinary) {
        try {
            return Parser.parse(wasmBinary);
        } catch (Exception e) {
            throw new WasmExecutionException("Failed to parse WASM binary: " + e.getMessage(), e);
        }
    }

    private Instance instantiateModule(WasmModule module) {
        try {
            // Create a store with AssemblyScript host functions
            Store store = new Store();
            
            // AssemblyScript requires env.abort for runtime errors
            // Signature: abort(messagePtr: i32, fileNamePtr: i32, line: i32, column: i32) -> void
            HostFunction abortFn = new HostFunction(
                "env",
                "abort",
                FunctionType.of(
                    List.of(ValType.I32, ValType.I32, ValType.I32, ValType.I32),
                    List.of()
                ),
                (Instance inst, long... args) -> {
                    int messagePtr = (int) args[0];
                    int line = (int) args[2];
                    int column = (int) args[3];
                    
                    String message = "abort";
                    try {
                        if (messagePtr != 0) {
                            message = stringCodec.readString(inst, messagePtr);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Could not read abort message: {}", e.getMessage());
                    }
                    
                    LOGGER.error("AssemblyScript abort called: {} at line {}, column {}", 
                        message, line, column);
                    throw new WasmExecutionException(
                        "AssemblyScript abort: " + message + " at line " + line);
                }
            );
            store.addFunction(abortFn);
            
            return store.instantiate("module", module);
        } catch (WasmExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new WasmExecutionException(
                "Failed to instantiate WASM module: " + e.getMessage(), e);
        }
    }

    private void checkMemoryUsage(Instance instance) {
        try {
            int pages = instance.memory().pages();
            int sizeKb = pages * 64;
            if (pages > MEMORY_WARNING_PAGES) {
                LOGGER.warn("WASM module requests {} pages ({}KB) of memory, " 
                    + "exceeds warning threshold of {} pages",
                    pages, sizeKb, MEMORY_WARNING_PAGES);
            } else {
                LOGGER.debug("WASM module memory: {} pages ({}KB)", pages, sizeKb);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not determine memory usage: {}", e.getMessage());
        }
    }

    private void validateHandleExport(Instance instance) throws WasmAbiException {
        try {
            instance.export("handle");
        } catch (Exception e) {
            throw new WasmAbiException(
                "Module must export a 'handle' function. " 
                + "Ensure your AssemblyScript code exports: "
                + "export function handle(input: string): string { ... }", e);
        }
    }

    private int executeWithTimeout(ExportFunction handle, int inputPtr) 
            throws WasmExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<long[]> future = executor.submit(() -> handle.apply(inputPtr));

        try {
            long[] result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return (int) result[0];

        } catch (TimeoutException e) {
            future.cancel(true); // Triggers thread interrupt
            throw new WasmExecutionException(
                "Execution timed out after " + timeout.toSeconds() + " seconds");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String message = sanitizeErrorMessage(cause);
            throw new WasmExecutionException("WASM trap: " + message, cause);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WasmExecutionException("Execution interrupted", e);

        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Sanitize error messages to include trap type but remove internal details.
     * 
     * <p>Examples:
     * <ul>
     *   <li>"unreachable" -> "unreachable instruction"</li>
     *   <li>"integer divide by zero" -> "integer divide by zero"</li>
     *   <li>Complex stack trace -> simplified message</li>
     * </ul>
     */
    private String sanitizeErrorMessage(Throwable cause) {
        if (cause == null) {
            return "unknown error";
        }

        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }

        // Extract first line and clean up common patterns
        String firstLine = message.lines().findFirst().orElse(message);
        
        // Remove package prefixes if present
        if (firstLine.contains(":")) {
            int colonIndex = firstLine.lastIndexOf(':');
            if (colonIndex < firstLine.length() - 1) {
                String afterColon = firstLine.substring(colonIndex + 1).trim();
                if (!afterColon.isEmpty()) {
                    firstLine = afterColon;
                }
            }
        }

        // Add context for common trap types
        if (firstLine.equalsIgnoreCase("unreachable")) {
            return "unreachable instruction";
        }

        return firstLine;
    }

    private String truncateForLog(String value) {
        if (value == null) {
            return "null";
        }
        if (value.length() <= 100) {
            return value;
        }
        return value.substring(0, 100) + "... (" + value.length() + " chars)";
    }
}
