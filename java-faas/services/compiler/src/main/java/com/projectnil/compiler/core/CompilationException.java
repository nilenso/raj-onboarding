package com.projectnil.compiler.core;

public class CompilationException extends Exception {
    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
