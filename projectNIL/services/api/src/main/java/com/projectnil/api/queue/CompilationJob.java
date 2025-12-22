package com.projectnil.api.queue;

import java.util.UUID;

public record CompilationJob(
    UUID functionId,
    String language,
    String source
) {}
