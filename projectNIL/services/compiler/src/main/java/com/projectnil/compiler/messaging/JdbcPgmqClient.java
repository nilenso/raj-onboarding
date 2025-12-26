package com.projectnil.compiler.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.common.domain.queue.CompilationResult;
import com.projectnil.compiler.config.CompilerProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcPgmqClient implements PgmqClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPgmqClient.class);

    private final JdbcTemplate jdbcTemplate;
    private final CompilerProperties compilerProperties;
    private final ObjectMapper objectMapper;

    public JdbcPgmqClient(
        JdbcTemplate jdbcTemplate,
        CompilerProperties compilerProperties,
        ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.compilerProperties = compilerProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<QueuedCompilationJob> readJob() {
        LOGGER.warn("pgmq client not yet implemented");
        return Optional.empty();
    }

    @Override
    public void deleteJob(long messageId) {
        LOGGER.warn("deleteJob is a stub for messageId={}", messageId);
    }

    @Override
    public void publishResult(CompilationResult result) {
        LOGGER.warn(
            "publishResult is a stub for functionId={}, success={}",
            result.functionId(),
            result.success()
        );
    }
}
