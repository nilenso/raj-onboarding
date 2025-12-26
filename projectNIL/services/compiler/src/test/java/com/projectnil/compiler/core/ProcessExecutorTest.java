package com.projectnil.compiler.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessExecutorTest {

    private final ProcessExecutor executor = new ProcessExecutor();

    @Test
    void executesSuccessfulCommand() throws IOException, InterruptedException {
        ProcessExecutor.ProcessResult result = executor.execute(
            List.of("/bin/sh", "-c", "echo hello"),
            Duration.ofSeconds(2)
        );

        assertThat(result.success()).isTrue();
        assertThat(result.stdout()).contains("hello");
        assertThat(result.stderr()).isBlank();
    }

    @Test
    void capturesFailureOutput() throws IOException, InterruptedException {
        ProcessExecutor.ProcessResult result = executor.execute(
            List.of("/bin/sh", "-c", "echo error >&2; exit 1"),
            Duration.ofSeconds(2)
        );

        assertThat(result.success()).isFalse();
        assertThat(result.stderr()).contains("error");
    }

    @Test
    void throwsOnTimeout() {
        assertThatThrownBy(() -> executor.execute(List.of("/bin/sh", "-c", "sleep 5"), Duration.ofMillis(100)))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("timed out");
    }
}
