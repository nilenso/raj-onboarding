package com.projectnil.compiler.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.projectnil.common.domain.queue.CompilationJob;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssemblyScriptCompilerTest {

    private WorkspaceManager workspaceManager;
    private ProcessExecutor processExecutor;
    private AssemblyScriptCompiler compiler;
    private Path workspace;

    @BeforeEach
    void setUp() throws IOException {
        workspaceManager = mock(WorkspaceManager.class);
        processExecutor = mock(ProcessExecutor.class);
        compiler = new AssemblyScriptCompiler(
            workspaceManager,
            processExecutor,
            Duration.ofSeconds(1),
            "asc"
        );
        workspace = Files.createTempDirectory("compiler-test");
    }

    @Test
    void compilesSuccessfully() throws Exception {
        UUID functionId = UUID.randomUUID();
        CompilationJob job = new CompilationJob(functionId, "assemblyscript", "export function add(a: i32, b: i32) { return a + b; }");
        Path sourceFile = workspace.resolve("module.ts");
        Path wasmFile = workspace.resolve("module.wasm");
        Files.writeString(wasmFile, "fake-wasm");

        when(workspaceManager.createWorkspace(functionId)).thenReturn(workspace);
        when(workspaceManager.writeSource(workspace, job.source())).thenReturn(sourceFile);
        when(workspaceManager.wasmFile(workspace)).thenReturn(wasmFile);
        when(processExecutor.execute(
            List.of("asc", sourceFile.toString(), "--binaryFile", wasmFile.toString(), "--optimize", "--measure"),
            Duration.ofSeconds(1)
        )).thenReturn(new ProcessExecutor.ProcessResult(0, "", ""));

        CompilationOutcome outcome = compiler.compile(job);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.wasmBinary()).isPresent();
        verify(workspaceManager).cleanup(workspace);
    }

    @Test
    void returnsFailureOutcomeWhenProcessFails() throws Exception {
        UUID functionId = UUID.randomUUID();
        CompilationJob job = new CompilationJob(functionId, "assemblyscript", "code");
        Path sourceFile = workspace.resolve("module.ts");
        Path wasmFile = workspace.resolve("module.wasm");

        when(workspaceManager.createWorkspace(functionId)).thenReturn(workspace);
        when(workspaceManager.writeSource(workspace, job.source())).thenReturn(sourceFile);
        when(workspaceManager.wasmFile(workspace)).thenReturn(wasmFile);
        when(processExecutor.execute(
            List.of("asc", sourceFile.toString(), "--binaryFile", wasmFile.toString(), "--optimize", "--measure"),
            Duration.ofSeconds(1)
        )).thenReturn(new ProcessExecutor.ProcessResult(1, "", "compile error"));

        CompilationOutcome outcome = compiler.compile(job);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.errorMessage()).contains("compile error");
        verify(workspaceManager).cleanup(workspace);
    }

    @Test
    void propagatesIOException() throws Exception {
        UUID functionId = UUID.randomUUID();
        CompilationJob job = new CompilationJob(functionId, "assemblyscript", "code");

        when(workspaceManager.createWorkspace(functionId)).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> compiler.compile(job))
            .isInstanceOf(CompilationException.class)
            .hasMessageContaining("Unable to create workspace");
    }
}
