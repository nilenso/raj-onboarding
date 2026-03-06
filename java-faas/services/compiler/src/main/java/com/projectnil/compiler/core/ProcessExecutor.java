package com.projectnil.compiler.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutor.class);

    public ProcessResult execute(List<String> command, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        // Set JSON_MODE for json-as library
        // Using SWAR mode as Chicory WASM runtime doesn't support SIMD instructions
        builder.environment().put("JSON_MODE", "SWAR");
        Process process = builder.start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Process timed out after " + timeout.toMillis() + "ms");
        }
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        LOGGER.debug("Process exited with code {}", process.exitValue());
        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private String readStream(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record ProcessResult(int exitCode, String stdout, String stderr) {
        public boolean success() {
            return exitCode == 0;
        }
    }
}
