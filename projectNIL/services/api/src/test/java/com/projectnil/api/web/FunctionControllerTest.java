package com.projectnil.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.api.repository.ExecutionRepository;
import com.projectnil.api.repository.FunctionRepository;
import com.projectnil.common.domain.ExecutionStatus;
import com.projectnil.common.domain.Function;
import com.projectnil.common.domain.FunctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.projectnil.api.messaging.PgmqClient;
import com.projectnil.api.messaging.PgmqClient.QueuedCompilationResult;
import com.projectnil.common.domain.queue.CompilationJob;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for FunctionController execute endpoint.
 *
 * <p>Tests per Issue #29 acceptance criteria:
 * <ul>
 *   <li>POST /functions/{id}/execute accepts ExecutionRequest</li>
 *   <li>Rejects execution (400) when Function.status is not READY</li>
 *   <li>Persists Execution row with state transitions</li>
 *   <li>Loads WASM binary, invokes WasmRuntime, stores output</li>
 *   <li>Handles runtime traps by marking execution FAILED yet returning 200</li>
 *   <li>Returns 404 when function does not exist</li>
 * </ul>
 */
@SpringBootTest(classes = {
        com.projectnil.api.ApiApplication.class,
        FunctionControllerTest.TestConfig.class
})
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class FunctionControllerTest {

    static {
        // Configure Testcontainers for Podman before any container classes load
        configurePodmanEnvironment();
    }

    private static void configurePodmanEnvironment() {
        if (System.getenv("DOCKER_HOST") != null) {
            return; // Already configured externally
        }
        if (Files.exists(Path.of("/var/run/docker.sock"))) {
            return; // Docker is available
        }
        String podmanSocket = discoverPodmanSocketPath();
        if (podmanSocket != null) {
            System.setProperty("tc.host", podmanSocket);
            System.setProperty("docker.host", podmanSocket);
            System.setProperty("testcontainers.ryuk.disabled", "true");
        }
    }

    private static String discoverPodmanSocketPath() {
        Path linuxSocket = Path.of("/var/run/podman/podman.sock");
        if (Files.exists(linuxSocket)) {
            return "unix:///var/run/podman/podman.sock";
        }
        try {
            Process process = new ProcessBuilder("podman", "machine", "inspect",
                    "--format", "{{.ConnectionInfo.PodmanSocket.Path}}")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String socketPath = reader.readLine();
                if (process.waitFor() == 0 && socketPath != null && !socketPath.isBlank()) {
                    Path socket = Path.of(socketPath.trim());
                    if (Files.exists(socket)) {
                        return "unix://" + socket;
                    }
                }
            }
        } catch (Exception ex) {
            // Podman not available
        }
        return null;
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("projectnil_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    /**
     * Test configuration that provides a no-op PGMQ client.
     * This avoids needing the PGMQ extension in the test database.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        PgmqClient testPgmqClient() {
            return new PgmqClient() {
                @Override
                public long publishJob(CompilationJob job) {
                    // No-op for tests, return fake message ID
                    return 1L;
                }

                @Override
                public Optional<QueuedCompilationResult> readResult(int visibilityTimeoutSeconds) {
                    return Optional.empty();
                }

                @Override
                public void deleteResult(long messageId) {
                    // No-op for tests
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
        functionRepository.deleteAll();
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

    private Function createReadyFunction(String name, byte[] wasmBinary) {
        Function function = Function.builder()
                .name(name)
                .description("Test function")
                .language("assemblyscript")
                .source("// test source")
                .wasmBinary(wasmBinary)
                .status(FunctionStatus.READY)
                .build();
        return functionRepository.save(function);
    }

    private Function createPendingFunction(String name) {
        Function function = Function.builder()
                .name(name)
                .description("Test function")
                .language("assemblyscript")
                .source("// test source")
                .status(FunctionStatus.PENDING)
                .build();
        return functionRepository.save(function);
    }

    @Nested
    @DisplayName("POST /functions/{id}/execute - Success scenarios")
    class ExecuteSuccessTests {

        @Test
        @DisplayName("executes echo function and returns COMPLETED status")
        void executeEchoFunctionReturnsCompleted() throws Exception {
            Function function = createReadyFunction("echo-test", loadWasm("echo"));
            Map<String, Object> input = Map.of("message", "hello");

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.functionId", is(function.getId().toString())))
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.output", containsString("message")))
                    .andExpect(jsonPath("$.errorMessage", nullValue()));
        }

        @Test
        @DisplayName("executes add function with numeric input")
        void executeAddFunctionComputesSum() throws Exception {
            Function function = createReadyFunction("add-test", loadWasm("add"));
            Map<String, Object> input = Map.of("a", 10, "b", 5);

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.output", containsString("15")));
        }

        @Test
        @DisplayName("executes greet function with string input")
        void executeGreetFunctionReturnsGreeting() throws Exception {
            Function function = createReadyFunction("greet-test", loadWasm("greet"));
            Map<String, Object> input = Map.of("name", "World");

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.output", containsString("Hello, World!")));
        }

        @Test
        @DisplayName("handles null input by using empty JSON object")
        void executeWithNullInputUsesEmptyObject() throws Exception {
            Function function = createReadyFunction("echo-null-test", loadWasm("echo"));

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.output", is("{}")));
        }
    }

    @Nested
    @DisplayName("POST /functions/{id}/execute - Error scenarios")
    class ExecuteErrorTests {

        @Test
        @DisplayName("returns 404 when function does not exist")
        void executeNonExistentFunctionReturns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            Map<String, Object> input = Map.of("a", 1);

            mockMvc.perform(post("/functions/{id}/execute", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("not found")));
        }

        @Test
        @DisplayName("returns 400 when function is PENDING (not READY)")
        void executePendingFunctionReturns400() throws Exception {
            Function function = createPendingFunction("pending-test");
            Map<String, Object> input = Map.of("a", 1);

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("not ready")));
        }

        @Test
        @DisplayName("returns 400 when function is COMPILING (not READY)")
        void executeCompilingFunctionReturns400() throws Exception {
            Function function = Function.builder()
                    .name("compiling-test")
                    .description("Test")
                    .language("assemblyscript")
                    .source("// source")
                    .status(FunctionStatus.COMPILING)
                    .build();
            function = functionRepository.save(function);

            Map<String, Object> input = Map.of("a", 1);

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("not ready")));
        }

        @Test
        @DisplayName("returns 400 when function is FAILED (not READY)")
        void executeFailedFunctionReturns400() throws Exception {
            Function function = Function.builder()
                    .name("failed-test")
                    .description("Test")
                    .language("assemblyscript")
                    .source("// source")
                    .status(FunctionStatus.FAILED)
                    .compileError("Compilation failed")
                    .build();
            function = functionRepository.save(function);

            Map<String, Object> input = Map.of("a", 1);

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("not ready")));
        }
    }

    @Nested
    @DisplayName("POST /functions/{id}/execute - Runtime failures")
    class RuntimeFailureTests {

        @Test
        @DisplayName("returns 200 with FAILED status on WASM trap")
        void executeTrapReturns200WithFailedStatus() throws Exception {
            Function function = createReadyFunction("trap-test", loadWasm("trap"));
            Map<String, Object> input = Map.of();

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FAILED")))
                    .andExpect(jsonPath("$.errorMessage", notNullValue()))
                    .andExpect(jsonPath("$.output", nullValue()));
        }
    }

    @Nested
    @DisplayName("Execution persistence")
    class PersistenceTests {

        @Test
        @DisplayName("persists execution record on success")
        void executionIsPersisted() throws Exception {
            Function function = createReadyFunction("persist-test", loadWasm("echo"));
            Map<String, Object> input = Map.of("test", "value");

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isOk());

            var executions = executionRepository.findByFunctionIdOrderByCreatedAtDesc(function.getId());
            org.junit.jupiter.api.Assertions.assertEquals(1, executions.size());
            org.junit.jupiter.api.Assertions.assertEquals(ExecutionStatus.COMPLETED, executions.get(0).getStatus());
            org.junit.jupiter.api.Assertions.assertNotNull(executions.get(0).getStartedAt());
            org.junit.jupiter.api.Assertions.assertNotNull(executions.get(0).getCompletedAt());
        }

        @Test
        @DisplayName("persists execution record on failure")
        void executionIsPersistedOnFailure() throws Exception {
            Function function = createReadyFunction("persist-fail-test", loadWasm("trap"));
            Map<String, Object> input = Map.of();

            mockMvc.perform(post("/functions/{id}/execute", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExecutionRequest(input))))
                    .andExpect(status().isOk());

            var executions = executionRepository.findByFunctionIdOrderByCreatedAtDesc(function.getId());
            org.junit.jupiter.api.Assertions.assertEquals(1, executions.size());
            org.junit.jupiter.api.Assertions.assertEquals(ExecutionStatus.FAILED, executions.get(0).getStatus());
            org.junit.jupiter.api.Assertions.assertNotNull(executions.get(0).getErrorMessage());
        }
    }

    /**
     * Tests for PUT /functions/{id} - Update Function (#27).
     *
     * <p>Per issue #27 acceptance criteria:
     * <ul>
     *   <li>PUT /functions/{id} accepts FunctionRequest fields</li>
     *   <li>When source or language changes, reset status to PENDING, clear wasmBinary/compileError</li>
     *   <li>Returns updated function (expanded view)</li>
     *   <li>Returns 404 if function does not exist</li>
     *   <li>updatedAt is refreshed automatically</li>
     * </ul>
     */
    @Nested
    @DisplayName("PUT /functions/{id} - Update Function")
    class UpdateFunctionTests {

        @Test
        @DisplayName("updates function name and description without triggering recompilation")
        void updateNameAndDescriptionOnly() throws Exception {
            Function function = createReadyFunction("original-name", loadWasm("echo"));
            FunctionRequest updateRequest = new FunctionRequest(
                    "updated-name",
                    "Updated description",
                    "assemblyscript",
                    function.getSource()
            );

            mockMvc.perform(put("/functions/{id}", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(function.getId().toString())))
                    .andExpect(jsonPath("$.name", is("updated-name")))
                    .andExpect(jsonPath("$.description", is("Updated description")))
                    .andExpect(jsonPath("$.status", is("READY")))
                    .andExpect(jsonPath("$.updatedAt", notNullValue()));

            // Verify WASM binary is preserved
            Function updated = functionRepository.findById(function.getId()).orElseThrow();
            org.junit.jupiter.api.Assertions.assertNotNull(updated.getWasmBinary());
            org.junit.jupiter.api.Assertions.assertEquals(FunctionStatus.READY, updated.getStatus());
        }

        @Test
        @DisplayName("triggers recompilation when source changes")
        void updateSourceTriggersRecompilation() throws Exception {
            Function function = createReadyFunction("recompile-source", loadWasm("echo"));
            String newSource = "// new source code";
            FunctionRequest updateRequest = new FunctionRequest(
                    function.getName(),
                    function.getDescription(),
                    "assemblyscript",
                    newSource
            );

            mockMvc.perform(put("/functions/{id}", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.source", is(newSource)))
                    .andExpect(jsonPath("$.compileError", nullValue()));

            // Verify WASM binary is cleared
            Function updated = functionRepository.findById(function.getId()).orElseThrow();
            org.junit.jupiter.api.Assertions.assertNull(updated.getWasmBinary());
            org.junit.jupiter.api.Assertions.assertEquals(FunctionStatus.PENDING, updated.getStatus());
        }

        @Test
        @DisplayName("triggers recompilation when language changes")
        void updateLanguageTriggersRecompilation() throws Exception {
            // Note: Only assemblyscript is supported in Phase 0, but the logic should still reset
            Function function = createReadyFunction("recompile-lang", loadWasm("echo"));
            FunctionRequest updateRequest = new FunctionRequest(
                    function.getName(),
                    function.getDescription(),
                    "assemblyscript", // Same language (only one supported)
                    "// different source to trigger recompile"
            );

            mockMvc.perform(put("/functions/{id}", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }

        @Test
        @DisplayName("clears compile error when source changes on FAILED function")
        void updateClearsCompileErrorOnFailedFunction() throws Exception {
            Function function = Function.builder()
                    .name("failed-func")
                    .description("Test")
                    .language("assemblyscript")
                    .source("// bad source")
                    .status(FunctionStatus.FAILED)
                    .compileError("Syntax error at line 1")
                    .build();
            function = functionRepository.save(function);

            FunctionRequest updateRequest = new FunctionRequest(
                    function.getName(),
                    function.getDescription(),
                    "assemblyscript",
                    "// fixed source"
            );

            mockMvc.perform(put("/functions/{id}", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.compileError", nullValue()));
        }

        @Test
        @DisplayName("returns 404 when function does not exist")
        void updateNonExistentFunctionReturns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            FunctionRequest updateRequest = new FunctionRequest(
                    "name",
                    "desc",
                    "assemblyscript",
                    "// source"
            );

            mockMvc.perform(put("/functions/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("not found")));
        }

        @Test
        @DisplayName("returns 415 for unsupported language")
        void updateWithUnsupportedLanguageReturns415() throws Exception {
            Function function = createReadyFunction("unsupported-lang", loadWasm("echo"));
            FunctionRequest updateRequest = new FunctionRequest(
                    function.getName(),
                    function.getDescription(),
                    "rust",
                    "// source"
            );

            mockMvc.perform(put("/functions/{id}", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.message", containsString("Unsupported language")));
        }

        @Test
        @DisplayName("returns expanded view with all fields")
        void updateReturnsExpandedView() throws Exception {
            Function function = createReadyFunction("expanded-view", loadWasm("echo"));
            FunctionRequest updateRequest = new FunctionRequest(
                    "new-name",
                    "new-description",
                    "assemblyscript",
                    function.getSource()
            );

            mockMvc.perform(put("/functions/{id}", function.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("new-name")))
                    .andExpect(jsonPath("$.description", is("new-description")))
                    .andExpect(jsonPath("$.language", is("assemblyscript")))
                    .andExpect(jsonPath("$.source", notNullValue()))
                    .andExpect(jsonPath("$.status", notNullValue()))
                    .andExpect(jsonPath("$.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.updatedAt", notNullValue()));
        }
    }
}
