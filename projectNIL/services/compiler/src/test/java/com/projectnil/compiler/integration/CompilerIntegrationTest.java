package com.projectnil.compiler.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.common.domain.queue.CompilationJob;
import com.projectnil.common.domain.queue.CompilationResult;
import com.projectnil.compiler.config.CompilerProperties;
import com.projectnil.compiler.core.AssemblyScriptCompiler;
import com.projectnil.compiler.core.CompilerRunner;
import com.projectnil.compiler.core.DefaultCompilerRunner;
import com.projectnil.compiler.core.FileSystemWorkspaceManager;
import com.projectnil.compiler.core.LanguageCompiler;
import com.projectnil.compiler.core.ProcessExecutor;
import com.projectnil.compiler.core.WorkspaceManager;
import com.projectnil.compiler.messaging.JdbcPgmqClient;
import com.projectnil.compiler.messaging.PgmqClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilerIntegrationTest {

    private static final DockerImageName PGMQ_IMAGE = DockerImageName
        .parse("ghcr.io/pgmq/pg18-pgmq:v1.8.0")
        .asCompatibleSubstituteFor("postgres");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PostgreSQLContainer<?> postgres;
    private JdbcTemplate jdbcTemplate;
    private CompilerRunner runner;
    private Path workspaceRoot;

    @BeforeAll
    void startContainer() {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker is required for integration tests");
        postgres = new PostgreSQLContainer<>(PGMQ_IMAGE)
            .withDatabaseName("projectnil")
            .withUsername("projectnil")
            .withPassword("projectnil");
        postgres.start();
    }

    @AfterAll
    void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pgmq");
        jdbcTemplate.execute("SELECT pgmq.create('compilation_jobs')");
        jdbcTemplate.execute("SELECT pgmq.create('compilation_results')");

        workspaceRoot = Files.createTempDirectory("compiler-it-workspace");
        Path ascBinary = Path.of(
            Objects.requireNonNull(getClass().getResource("/bin/asc")).toURI()
        );

        CompilerProperties compilerProperties = new CompilerProperties(
            "assemblyscript",
            "compilation_jobs",
            "compilation_results",
            10_000L,
            200L,
            ascBinary.toString(),
            workspaceRoot.toString()
        );

        WorkspaceManager workspaceManager = new FileSystemWorkspaceManager(workspaceRoot);
        ProcessExecutor processExecutor = new ProcessExecutor();
        LanguageCompiler languageCompiler = new AssemblyScriptCompiler(
            workspaceManager,
            processExecutor,
            Duration.ofSeconds(5),
            ascBinary.toString()
        );
        PgmqClient pgmqClient = new JdbcPgmqClient(jdbcTemplate, compilerProperties, objectMapper);
        runner = new DefaultCompilerRunner(pgmqClient, languageCompiler, compilerProperties);
        runner.start();
    }

    @AfterEach
    void tearDown() {
        if (runner != null) {
            runner.stop();
        }
        if (workspaceRoot != null) {
            FileSystemUtils.deleteRecursively(workspaceRoot.toFile());
        }
    }

    @Test
    void processesCompilationJobsAndPublishesResults() throws Exception {
        UUID successId = UUID.randomUUID();
        UUID failureId = UUID.randomUUID();

        enqueueJob(new CompilationJob(successId, "assemblyscript", "export function ok() { return 1; }"));
        enqueueJob(new CompilationJob(failureId, "assemblyscript", "export function fail() { return 1; }"));

        CompilationResult successResult = awaitResult(successId);
        CompilationResult failureResult = awaitResult(failureId);

        assertThat(successResult.success()).isTrue();
        assertThat(successResult.wasmBinary()).isNotNull();
        assertThat(new String(successResult.wasmBinary(), StandardCharsets.UTF_8))
            .isEqualTo(Base64.getEncoder().encodeToString("fake wasm".getBytes(StandardCharsets.UTF_8)));
        assertThat(successResult.error()).isNull();

        assertThat(failureResult.success()).isFalse();
        assertThat(failureResult.wasmBinary()).isNull();
        assertThat(failureResult.error()).contains("compile error");
    }

    private void enqueueJob(CompilationJob job) throws Exception {
        PGobject payload = new PGobject();
        payload.setType("jsonb");
        payload.setValue(objectMapper.writeValueAsString(job));
        jdbcTemplate.query(
            "SELECT pgmq.send(?, ?)",
            ps -> {
                ps.setString(1, "compilation_jobs");
                ps.setObject(2, payload);
            },
            rs -> { }
        );
    }

    private CompilationResult awaitResult(UUID functionId) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (System.currentTimeMillis() < deadline) {
            List<ResultMessage> messages = jdbcTemplate.query(
                "SELECT msg_id, message FROM pgmq.read(?, ?, 10)",
                ps -> {
                    ps.setString(1, "compilation_results");
                    ps.setInt(2, 1);
                },
                (rs, rowNum) -> new ResultMessage(rs.getLong("msg_id"), rs.getString("message"))
            );
            Optional<CompilationResult> match = messages
                .stream()
                .map(this::toCompilationResult)
                .filter(result -> result.functionId().equals(functionId))
                .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Compilation result not received for " + functionId);
    }

    private CompilationResult toCompilationResult(ResultMessage message) {
        try {
            CompilationResult result = objectMapper.readValue(message.payload(), CompilationResult.class);
            jdbcTemplate.query("SELECT pgmq.delete(?, ?)", ps -> {
                ps.setString(1, "compilation_results");
                ps.setLong(2, message.msgId());
            }, rs -> { });
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse result payload", ex);
        }
    }

    private boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private record ResultMessage(long msgId, String payload) {}
}
