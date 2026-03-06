package com.projectnil.common.domain.queue;

import java.util.UUID;

public record CompilationJob(
    UUID functionId,
    String language,
    String source
) {}
