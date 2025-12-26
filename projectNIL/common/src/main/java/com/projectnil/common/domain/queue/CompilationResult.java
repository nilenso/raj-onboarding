package com.projectnil.common.domain.queue;

import java.util.UUID;

public record CompilationResult(
    UUID functionId,
    boolean success,
    byte[] wasmBinary,
    String error
) {}
