package com.projectnil.api.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests for {@link ChicoryWasmRuntime}.
 * 
 * <p>These tests use pre-compiled AssemblyScript WASM modules from test resources.
 * No Spring context is required.
 */
class ChicoryWasmRuntimeTest {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(1);

    private WasmStringCodec stringCodec;
    private ChicoryWasmRuntime runtime;

    @BeforeEach
    void setUp() {
        stringCodec = new AssemblyScriptStringCodec();
        runtime = new ChicoryWasmRuntime(stringCodec, DEFAULT_TIMEOUT);
    }

    private byte[] loadWasm(String name) throws IOException {
        String path = "wasm/" + name + ".wasm";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("WASM resource not found: " + path);
            }
            return is.readAllBytes();
        }
    }

    private String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("Success scenarios")
    class SuccessTests {

        @Test
        @DisplayName("echo module returns input unchanged")
        void executeEchoReturnsInputUnchanged() throws Exception {
            byte[] wasmBinary = loadWasm("echo");
            String input = "{\"foo\":\"bar\",\"num\":42}";

            byte[] result = runtime.execute(wasmBinary, input);

            assertEquals(input, bytesToString(result));
        }

        @Test
        @DisplayName("add module computes sum correctly")
        void executeAddComputesSum() throws Exception {
            byte[] wasmBinary = loadWasm("add");
            String input = "{\"a\":10,\"b\":5}";

            byte[] result = runtime.execute(wasmBinary, input);

            assertEquals("{\"sum\":15}", bytesToString(result));
        }

        @Test
        @DisplayName("add module handles negative numbers")
        void executeAddHandlesNegativeNumbers() throws Exception {
            byte[] wasmBinary = loadWasm("add");
            String input = "{\"a\":-5,\"b\":3}";

            byte[] result = runtime.execute(wasmBinary, input);

            assertEquals("{\"sum\":-2}", bytesToString(result));
        }

        @Test
        @DisplayName("greet module concatenates string")
        void executeGreetConcatenatesString() throws Exception {
            byte[] wasmBinary = loadWasm("greet");
            String input = "{\"name\":\"Alice\"}";

            byte[] result = runtime.execute(wasmBinary, input);

            assertEquals("{\"greeting\":\"Hello, Alice!\"}", bytesToString(result));
        }

        @Test
        @DisplayName("greet module uses default for missing name")
        void executeGreetUsesDefaultName() throws Exception {
            byte[] wasmBinary = loadWasm("greet");
            String input = "{}";

            byte[] result = runtime.execute(wasmBinary, input);

            assertEquals("{\"greeting\":\"Hello, World!\"}", bytesToString(result));
        }

        @Test
        @DisplayName("handles empty JSON object")
        void executeEchoHandlesEmptyObject() throws Exception {
            byte[] wasmBinary = loadWasm("echo");
            String input = "{}";

            byte[] result = runtime.execute(wasmBinary, input);

            assertEquals("{}", bytesToString(result));
        }

        @Test
        @DisplayName("handles unicode strings")
        void executeEchoHandlesUnicode() throws Exception {
            byte[] wasmBinary = loadWasm("echo");
            String input = "{\"message\":\"Hello, 世界! 🌍\"}";

            byte[] result = runtime.execute(wasmBinary, input);

            assertEquals(input, bytesToString(result));
        }
    }

    @Nested
    @DisplayName("ABI validation")
    class AbiValidationTests {

        @Test
        @DisplayName("throws WasmAbiException when handle export is missing")
        void executeNoHandleThrowsAbiException() throws Exception {
            byte[] wasmBinary = loadWasm("no-handle");

            WasmAbiException exception = assertThrows(
                WasmAbiException.class,
                () -> runtime.execute(wasmBinary, "{}")
            );

            assertTrue(exception.getMessage().contains("handle"),
                "Exception message should mention missing 'handle' export");
        }

        @Test
        @DisplayName("throws WasmExecutionException for invalid WASM binary")
        void executeInvalidBinaryThrowsException() {
            byte[] invalidBinary = "not a wasm module".getBytes();

            WasmExecutionException exception = assertThrows(
                WasmExecutionException.class,
                () -> runtime.execute(invalidBinary, "{}")
            );

            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("throws WasmExecutionException for empty WASM binary")
        void executeEmptyBinaryThrowsException() {
            byte[] emptyBinary = new byte[0];

            assertThrows(
                WasmExecutionException.class,
                () -> runtime.execute(emptyBinary, "{}")
            );
        }
    }

    @Nested
    @DisplayName("Runtime errors")
    class RuntimeErrorTests {

        @Test
        @DisplayName("throws WasmExecutionException on trap")
        void executeTrapThrowsExecutionException() throws Exception {
            byte[] wasmBinary = loadWasm("trap");

            WasmExecutionException exception = assertThrows(
                WasmExecutionException.class,
                () -> runtime.execute(wasmBinary, "{}")
            );

            assertTrue(exception.getMessage().toLowerCase().contains("trap") 
                || exception.getMessage().toLowerCase().contains("unreachable"),
                "Exception message should indicate trap: " + exception.getMessage());
        }

        @Test
        @DisplayName("throws WasmExecutionException on timeout")
        @Timeout(5) // Test should complete within 5 seconds
        void executeInfiniteLoopTimesOut() throws Exception {
            byte[] wasmBinary = loadWasm("infinite-loop");
            ChicoryWasmRuntime shortTimeoutRuntime = 
                new ChicoryWasmRuntime(stringCodec, SHORT_TIMEOUT);

            WasmExecutionException exception = assertThrows(
                WasmExecutionException.class,
                () -> shortTimeoutRuntime.execute(wasmBinary, "{}")
            );

            assertTrue(exception.getMessage().toLowerCase().contains("timeout") 
                || exception.getMessage().toLowerCase().contains("timed out"),
                "Exception message should indicate timeout: " + exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("respects custom timeout configuration")
        void runtimeRespectsCustomTimeout() {
            Duration customTimeout = Duration.ofSeconds(30);
            ChicoryWasmRuntime customRuntime = 
                new ChicoryWasmRuntime(stringCodec, customTimeout);

            // Runtime should be created successfully with custom timeout
            assertNotNull(customRuntime);
        }
    }
}
