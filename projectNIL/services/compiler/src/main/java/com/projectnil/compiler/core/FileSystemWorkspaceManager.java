package com.projectnil.compiler.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

public class FileSystemWorkspaceManager implements WorkspaceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemWorkspaceManager.class);

    private final Path baseDirectory;

    public FileSystemWorkspaceManager(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public Path createWorkspace(UUID functionId) throws IOException {
        Path workspace = baseDirectory.resolve(functionId.toString());
        Files.createDirectories(workspace);
        Files.createDirectories(outputDirectory(workspace));
        return workspace;
    }

    @Override
    public Path writeSource(Path workspace, String source) throws IOException {
        Path sourceFile = workspace.resolve("module.ts");
        Files.writeString(
            sourceFile,
            source,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
        return sourceFile;
    }

    @Override
    public Path outputDirectory(Path workspace) {
        return workspace.resolve("out");
    }

    @Override
    public Path wasmFile(Path workspace) {
        return outputDirectory(workspace).resolve("module.wasm");
    }

    @Override
    public void cleanup(Path workspace) {
        if (workspace == null) {
            return;
        }
        try {
            FileSystemUtils.deleteRecursively(workspace);
        } catch (IOException ex) {
            LOGGER.warn("Failed to cleanup workspace {}", workspace, ex);
        }
    }
}
