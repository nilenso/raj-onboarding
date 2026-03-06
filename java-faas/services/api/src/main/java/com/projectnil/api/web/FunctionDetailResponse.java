package com.projectnil.api.web;

import com.projectnil.common.domain.FunctionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Expanded function response DTO.
 *
 * <p>Per scope/contracts.md, this is the expanded view returned for:
 * <ul>
 *   <li>GET /functions/{id}</li>
 *   <li>PUT /functions/{id}</li>
 * </ul>
 */
public record FunctionDetailResponse(
    UUID id,
    String name,
    String description,
    String language,
    String source,
    FunctionStatus status,
    String compileError,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
