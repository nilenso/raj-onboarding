package com.projectnil.compiler.core;

import com.projectnil.common.domain.queue.CompilationJob;

public interface LanguageCompiler {
    /**
     * @return the language identifier this compiler supports (e.g. "assemblyscript").
     */
    String language();

    /**
     * Compile the provided job payload into a WASM artifact.
     *
     * @param job the compilation job description
     * @return outcome describing success/failure and artifacts
     * @throws CompilationException when compilation cannot be completed
     */
    CompilationOutcome compile(CompilationJob job) throws CompilationException;
}
