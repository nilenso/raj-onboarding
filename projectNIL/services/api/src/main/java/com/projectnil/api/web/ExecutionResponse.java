package com.projectnil.api.web;

import com.projectnil.common.domain.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExecutionResponse(
    UUID id,
    UUID functionId,
    ExecutionStatus status,
    String output,
    String errorMessage,
    LocalDateTime createdAt
) {}
