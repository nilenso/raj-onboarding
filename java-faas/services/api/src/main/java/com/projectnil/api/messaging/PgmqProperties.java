package com.projectnil.api.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for PGMQ integration.
 *
 * @param jobQueue the queue name for compilation jobs
 * @param resultQueue the queue name for compilation results
 * @param pollIntervalMs how often to poll for results (milliseconds)
 * @param visibilityTimeoutSeconds how long to hide messages from other readers
 */
@ConfigurationProperties(prefix = "projectnil.pgmq")
public record PgmqProperties(
        String jobQueue,
        String resultQueue,
        long pollIntervalMs,
        int visibilityTimeoutSeconds
) {
    public PgmqProperties {
        if (jobQueue == null || jobQueue.isBlank()) {
            jobQueue = "compilation_jobs";
        }
        if (resultQueue == null || resultQueue.isBlank()) {
            resultQueue = "compilation_results";
        }
        if (pollIntervalMs <= 0) {
            pollIntervalMs = 1000;
        }
        if (visibilityTimeoutSeconds <= 0) {
            visibilityTimeoutSeconds = 30;
        }
    }
}
