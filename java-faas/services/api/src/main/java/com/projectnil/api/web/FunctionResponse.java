package com.projectnil.api.web;

import com.projectnil.common.domain.FunctionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record FunctionResponse(
    UUID id,
    String name,
    FunctionStatus status,
    LocalDateTime createdAt
) {}
