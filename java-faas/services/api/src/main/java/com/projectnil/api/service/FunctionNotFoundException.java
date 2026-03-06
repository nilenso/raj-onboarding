package com.projectnil.api.service;

import java.util.UUID;

/**
 * Exception thrown when a function is not found.
 */
public class FunctionNotFoundException extends RuntimeException {

    private final UUID functionId;

    public FunctionNotFoundException(UUID functionId) {
        super("Function not found: " + functionId);
        this.functionId = functionId;
    }

    public UUID getFunctionId() {
        return functionId;
    }
}
