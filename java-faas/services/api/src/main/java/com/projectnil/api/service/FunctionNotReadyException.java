package com.projectnil.api.service;

import com.projectnil.common.domain.FunctionStatus;

import java.util.UUID;

/**
 * Exception thrown when attempting to execute a function that is not in READY status.
 */
public class FunctionNotReadyException extends RuntimeException {

    private final UUID functionId;
    private final FunctionStatus currentStatus;

    public FunctionNotReadyException(UUID functionId, FunctionStatus currentStatus) {
        super("Function " + functionId + " is not ready for execution. Current status: " + currentStatus);
        this.functionId = functionId;
        this.currentStatus = currentStatus;
    }

    public UUID getFunctionId() {
        return functionId;
    }

    public FunctionStatus getCurrentStatus() {
        return currentStatus;
    }
}
