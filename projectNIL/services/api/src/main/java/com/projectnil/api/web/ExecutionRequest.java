package com.projectnil.api.web;

/**
 * Request DTO for executing a function.
 * The input field accepts any JSON object which will be passed to the WASM function.
 *
 * @param input JSON object to pass as input to the function
 */
public record ExecutionRequest(Object input) {}
