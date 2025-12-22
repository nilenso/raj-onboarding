package com.projectnil.api.queue;

import java.util.UUID;

public record CompilationResult(
    UUID functionId,
    boolean success,
    byte[] wasmBinary,
    String error
) {}
