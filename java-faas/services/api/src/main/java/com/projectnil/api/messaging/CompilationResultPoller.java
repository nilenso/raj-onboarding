package com.projectnil.api.messaging;

import com.projectnil.api.service.CompilationResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background poller that consumes compilation results from PGMQ.
 *
 * <p>Per Issue #53, this component:
 * <ul>
 *   <li>Polls {@code compilation_results} queue at a configurable interval</li>
 *   <li>Applies results to functions via {@link CompilationResultHandler}</li>
 *   <li>Deletes messages only after successful processing</li>
 *   <li>Allows redelivery on transient failures</li>
 * </ul>
 */
@Component
public class CompilationResultPoller {

    private static final Logger LOG = LoggerFactory.getLogger(CompilationResultPoller.class);

    private final PgmqClient pgmqClient;
    private final PgmqProperties properties;
    private final CompilationResultHandler resultHandler;

    public CompilationResultPoller(
            PgmqClient pgmqClient,
            PgmqProperties properties,
            CompilationResultHandler resultHandler) {
        this.pgmqClient = pgmqClient;
        this.properties = properties;
        this.resultHandler = resultHandler;
    }

    /**
     * Poll for compilation results and process them.
     *
     * <p>Runs on a fixed delay to avoid overlapping executions.
     * Messages are deleted only after successful processing.
     */
    @Scheduled(fixedDelayString = "${projectnil.pgmq.poll-interval-ms:1000}")
    public void pollResults() {
        try {
            pgmqClient.readResult(properties.visibilityTimeoutSeconds())
                    .ifPresent(this::processResult);
        } catch (Exception ex) {
            LOG.error("Error polling compilation results", ex);
        }
    }

    private void processResult(PgmqClient.QueuedCompilationResult queued) {
        long messageId = queued.messageId();
        var result = queued.result();

        LOG.debug("Processing compilation result messageId={} functionId={} success={}",
                messageId, result.functionId(), result.success());

        try {
            boolean applied = resultHandler.applyResult(result);

            if (applied) {
                pgmqClient.deleteResult(messageId);
                LOG.info("compilation.result.applied functionId={} success={} messageId={}",
                        result.functionId(), result.success(), messageId);
            } else {
                // Result was not applied (function not found or already processed)
                // Delete the message to prevent infinite redelivery
                pgmqClient.deleteResult(messageId);
                LOG.warn("compilation.result.skipped functionId={} messageId={}",
                        result.functionId(), messageId);
            }

        } catch (Exception ex) {
            // Don't delete message - allow redelivery after visibility timeout
            LOG.error("Failed to apply compilation result functionId={} messageId={}",
                    result.functionId(), messageId, ex);
        }
    }
}
