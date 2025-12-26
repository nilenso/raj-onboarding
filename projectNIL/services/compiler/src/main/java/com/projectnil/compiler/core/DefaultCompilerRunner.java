package com.projectnil.compiler.core;

import com.projectnil.common.domain.queue.CompilationJob;
import com.projectnil.common.domain.queue.CompilationResult;
import com.projectnil.compiler.config.CompilerProperties;
import com.projectnil.compiler.messaging.PgmqClient;
import com.projectnil.compiler.messaging.QueuedCompilationJob;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCompilerRunner implements CompilerRunner, Runnable {


    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCompilerRunner.class);

    private final PgmqClient pgmqClient;
    private final LanguageCompiler languageCompiler;
    private final CompilerProperties compilerProperties;
    private final ExecutorService executorService;

    private final AtomicBoolean running;

    public DefaultCompilerRunner(
        PgmqClient pgmqClient,
        LanguageCompiler languageCompiler,
        CompilerProperties compilerProperties
    ) {
        this.pgmqClient = pgmqClient;
        this.languageCompiler = languageCompiler;
        this.compilerProperties = compilerProperties;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "compiler-runner");
            thread.setDaemon(true);
            return thread;
        });
        this.running = new AtomicBoolean(false);
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executorService.submit(this);
        LOGGER.info("Compiler runner started for language {}", languageCompiler.language());
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Compiler runner stopped");
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                Optional<QueuedCompilationJob> queuedJob = pgmqClient.readJob();
                if (queuedJob.isEmpty()) {
                    sleepQuietly();
                    continue;
                }
                processJob(queuedJob.get());
            } catch (Exception ex) {
                LOGGER.error("Unexpected error in compiler runner", ex);
                sleepQuietly();
            }
        }
    }

    private void processJob(QueuedCompilationJob queuedJob) {
        CompilationJob job = queuedJob.job();
        if (!Objects.equals(job.language(), languageCompiler.language())) {
            LOGGER.debug(
                "Skipping job {} because language {} does not match runner language {}",
                job.functionId(),
                job.language(),
                languageCompiler.language()
            );
            pgmqClient.deleteJob(queuedJob.messageId());
            return;
        }
        LOGGER.info("Processing compilation job for function {}", job.functionId());
        long start = System.currentTimeMillis();
        try {
            CompilationOutcome outcome = languageCompiler.compile(job);
            long duration = System.currentTimeMillis() - start;
            publishResult(job, outcome, duration);
        } catch (CompilationException ex) {
            long duration = System.currentTimeMillis() - start;
            LOGGER.warn("Compilation failed for function {}", job.functionId(), ex);
            publishFailure(job, ex.getMessage(), duration);
        } finally {
            pgmqClient.deleteJob(queuedJob.messageId());
        }
    }

    private void publishResult(CompilationJob job, CompilationOutcome outcome, long duration) {
        CompilationResult result = new CompilationResult(
            job.functionId(),
            outcome.success(),
            outcome.wasmBinary().map(Base64.getEncoder()::encode).orElse(null),
            outcome.errorMessage().orElse(null)
        );
        pgmqClient.publishResult(result);
        LOGGER.info(
            "Published compilation result for function {} (success={}, duration={}ms)",
            job.functionId(),
            outcome.success(),
            duration
        );
    }

    private void publishFailure(CompilationJob job, String errorMessage, long duration) {
        CompilationResult result = new CompilationResult(job.functionId(), false, null, errorMessage);
        pgmqClient.publishResult(result);
        LOGGER.info(
            "Published failure result for function {} (duration={}ms)",
            job.functionId(),
            duration
        );
    }

    private void sleepQuietly() {
        long interval = Math.max(compilerProperties.pollIntervalMs(), 100L);
        try {
            Thread.sleep(interval);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
