package com.projectnil.api.web;

import com.projectnil.common.domain.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed execution response DTO.
 *
 * <p>Per scope/contracts.md and issue #30, includes all fields for inspection:
 * <ul>
 *   <li>id, functionId, status</li>
 *   <li>input, output (JSON strings)</li>
 *   <li>errorMessage (only populated if FAILED)</li>
 *   <li>startedAt, completedAt, createdAt</li>
 * </ul>
 */
public record ExecutionDetailResponse(
    UUID id,
    UUID functionId,
    ExecutionStatus status,
    String input,
    String output,
    String errorMessage,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt
) {}
