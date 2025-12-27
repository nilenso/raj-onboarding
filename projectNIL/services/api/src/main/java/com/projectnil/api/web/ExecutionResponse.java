package com.projectnil.api.web;

import com.projectnil.common.domain.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for function execution.
 *
 * <p>Per scope/contracts.md, output is returned as a parsed JSON object, not a string.
 *
 * @param id the execution ID
 * @param functionId the function ID
 * @param status the execution status
 * @param output the parsed JSON output (null if failed)
 * @param errorMessage error message (only for FAILED status)
 * @param createdAt when the execution was created
 */
public record ExecutionResponse(
    UUID id,
    UUID functionId,
    ExecutionStatus status,
    Object output,
    String errorMessage,
    LocalDateTime createdAt
) {}
