package com.projectnil.compiler.core;

import java.util.Optional;

public record CompilationOutcome(
    boolean success,
    Optional<byte[]> wasmBinary,
    Optional<String> errorMessage,
    long durationMillis
) {
    public static CompilationOutcome success(byte[] wasmBinary, long durationMillis) {
        return new CompilationOutcome(true, Optional.of(wasmBinary), Optional.empty(), durationMillis);
    }

    public static CompilationOutcome failure(String errorMessage, long durationMillis) {
        return new CompilationOutcome(false, Optional.empty(), Optional.ofNullable(errorMessage), durationMillis);
    }
}
