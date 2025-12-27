package com.projectnil.api.messaging;

import com.projectnil.common.domain.queue.CompilationJob;
import com.projectnil.common.domain.queue.CompilationResult;

import java.util.Optional;

/**
 * Client interface for PGMQ operations.
 *
 * <p>The API service uses this to:
 * <ul>
 *   <li>Publish {@link CompilationJob} messages to the jobs queue</li>
 *   <li>Read {@link CompilationResult} messages from the results queue</li>
 * </ul>
 */
public interface PgmqClient {

    /**
     * Publish a compilation job to the jobs queue.
     *
     * @param job the compilation job
     * @return the message ID
     */
    long publishJob(CompilationJob job);

    /**
     * Read a compilation result from the results queue.
     *
     * @param visibilityTimeoutSeconds how long to hide the message from other readers
     * @return the queued result, or empty if no messages available
     */
    Optional<QueuedCompilationResult> readResult(int visibilityTimeoutSeconds);

    /**
     * Delete a message from the results queue after successful processing.
     *
     * @param messageId the message ID to delete
     */
    void deleteResult(long messageId);

    /**
     * A compilation result with its queue message ID.
     */
    record QueuedCompilationResult(long messageId, CompilationResult result) {}
}
