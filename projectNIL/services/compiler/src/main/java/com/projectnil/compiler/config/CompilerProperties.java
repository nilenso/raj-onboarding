package com.projectnil.compiler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "compiler")
public record CompilerProperties(
    String language,
    String jobQueue,
    String resultQueue,
    long timeoutMs,
    long pollIntervalMs,
    String ascBinary,
    String workspaceDir,
    String ascLibPath
) {}
