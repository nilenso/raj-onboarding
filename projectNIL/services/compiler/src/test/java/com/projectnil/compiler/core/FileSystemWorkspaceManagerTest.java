package com.projectnil.compiler.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileSystemWorkspaceManagerTest {

    private Path baseDir;
    private FileSystemWorkspaceManager workspaceManager;

    @BeforeEach
    void setUp() throws IOException {
        baseDir = Files.createTempDirectory("compiler-test");
        workspaceManager = new FileSystemWorkspaceManager(baseDir);
    }

    @AfterEach
    void tearDown() {
        workspaceManager.cleanup(baseDir);
    }

    @Test
    void createsWorkspaceAndOutputDirectories() throws IOException {
        UUID functionId = UUID.randomUUID();

        Path workspace = workspaceManager.createWorkspace(functionId);

        assertThat(workspace).exists().isDirectory();
        assertThat(workspaceManager.outputDirectory(workspace)).exists().isDirectory();
    }

    @Test
    void writesSourceFile() throws IOException {
        UUID functionId = UUID.randomUUID();
        Path workspace = workspaceManager.createWorkspace(functionId);

        Path sourceFile = workspaceManager.writeSource(workspace, "export function add(a: i32, b: i32) { return a + b; }");

        assertThat(sourceFile)
            .exists()
            .hasContent("export function add(a: i32, b: i32) { return a + b; }");
    }

    @Test
    void cleanupRemovesWorkspace() throws IOException {
        UUID functionId = UUID.randomUUID();
        Path workspace = workspaceManager.createWorkspace(functionId);

        workspaceManager.cleanup(workspace);

        assertThat(workspace).doesNotExist();
    }
}
