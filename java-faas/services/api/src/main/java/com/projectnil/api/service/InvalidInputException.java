package com.projectnil.api.service;

/**
 * Exception thrown when execution input is invalid.
 *
 * <p>Per scope/contracts.md, ExecutionRequest.input must be a JSON object.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
