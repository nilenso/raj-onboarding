package com.projectnil.compiler.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface WorkspaceManager {
    Path createWorkspace(UUID functionId) throws IOException;

    Path writeSource(Path workspace, String source) throws IOException;

    Path readWasm(Path workspace) throws IOException;

    void cleanup(Path workspace);
}
