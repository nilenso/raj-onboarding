package com.projectnil.compiler.messaging;

import com.projectnil.common.domain.queue.CompilationJob;

public record QueuedCompilationJob(long messageId, CompilationJob job) {}
