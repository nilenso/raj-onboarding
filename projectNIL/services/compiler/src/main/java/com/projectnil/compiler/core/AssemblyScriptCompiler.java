package com.projectnil.compiler.core;

import com.projectnil.common.domain.queue.CompilationJob;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssemblyScriptCompiler implements LanguageCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssemblyScriptCompiler.class);
    private static final String WASM_FILENAME = "module.wasm";

    private final WorkspaceManager workspaceManager;
    private final ProcessExecutor processExecutor;
    private final Duration timeout;
    private final String ascBinary;

    public AssemblyScriptCompiler(
        WorkspaceManager workspaceManager,
        ProcessExecutor processExecutor,
        Duration timeout,
        String ascBinary
    ) {
        this.workspaceManager = workspaceManager;
        this.processExecutor = processExecutor;
        this.timeout = timeout;
        this.ascBinary = ascBinary;
    }

    @Override
    public String language() {
        return "assemblyscript";
    }

    @Override
    public CompilationOutcome compile(CompilationJob job) throws CompilationException {
        Path workspace;
        try {
            workspace = workspaceManager.createWorkspace(job.functionId());
        } catch (IOException ex) {
            throw new CompilationException("Unable to create workspace", ex);
        }

        long start = System.currentTimeMillis();
        try {
            Path sourceFile = workspaceManager.writeSource(workspace, job.source());
            List<String> command = ascCommand(sourceFile, workspaceManager.wasmFile(workspace));
            ProcessExecutor.ProcessResult result = processExecutor.execute(command, timeout);
            long duration = System.currentTimeMillis() - start;
            if (!result.success()) {
                return CompilationOutcome.failure(result.stderr(), duration);
            }
            byte[] wasmBytes = Files.readAllBytes(workspaceManager.wasmFile(workspace));
            return CompilationOutcome.success(wasmBytes, duration);
        } catch (IOException ex) {
            throw new CompilationException("Failed to compile AssemblyScript source", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CompilationException("Compilation interrupted", ex);
        } finally {
            workspaceManager.cleanup(workspace);
        }
    }

    private List<String> ascCommand(Path sourceFile, Path wasmFile) {
        List<String> command = new ArrayList<>();
        command.add(ascBinary);
        command.add(sourceFile.toString());
        command.add("--binaryFile");
        command.add(wasmFile.toString());
        command.add("--optimize");
        command.add("--measure");
        return command;
    }
}
