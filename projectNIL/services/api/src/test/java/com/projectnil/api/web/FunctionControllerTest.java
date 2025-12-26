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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@SpringBootTest
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
}
