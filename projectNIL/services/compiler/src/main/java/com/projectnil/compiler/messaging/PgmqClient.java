package com.projectnil.compiler.messaging;

import com.projectnil.common.domain.queue.CompilationJob;
import com.projectnil.common.domain.queue.CompilationResult;
import java.util.Optional;

public interface PgmqClient {
    Optional<QueuedCompilationJob> readJob();

    void deleteJob(long messageId);

    void publishResult(CompilationResult result);
}

