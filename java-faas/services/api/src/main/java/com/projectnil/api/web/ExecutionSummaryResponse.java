package com.projectnil.api.web;

import com.projectnil.common.domain.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight execution summary for list responses.
 *
 * <p>Per scope/contracts.md and issue #31, excludes heavy fields (input, output)
 * to keep list responses lightweight.
 */
public record ExecutionSummaryResponse(
    UUID id,
    ExecutionStatus status,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {}
