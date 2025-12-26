package com.projectnil.compiler.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.common.domain.queue.CompilationJob;
import com.projectnil.common.domain.queue.CompilationResult;
import com.projectnil.compiler.config.CompilerProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcPgmqClient implements PgmqClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPgmqClient.class);
    private static final String READ_SQL = "SELECT msg_id, message FROM pgmq.read(?, ?, 1)";
    private static final String DELETE_SQL = "SELECT pgmq.delete(?, ?)";
    private static final String SEND_SQL = "SELECT pgmq.send(?, ?)";
    private static final int DEFAULT_VT_SECONDS = 30;

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
        try {
            List<QueuedCompilationJob> jobs = jdbcTemplate.query(
                READ_SQL,
                ps -> {
                    ps.setString(1, compilerProperties.jobQueue());
                    ps.setInt(2, visibilityTimeoutSeconds());
                },
                this::mapQueuedCompilationJob
            );
            return jobs.stream().findFirst();
        } catch (DataAccessException ex) {
            LOGGER.error("Failed to read job from queue {}", compilerProperties.jobQueue(), ex);
            return Optional.empty();
        }
    }

    @Override
    public void deleteJob(long messageId) {
        try {
            Boolean deleted = jdbcTemplate.queryForObject(
                DELETE_SQL,
                Boolean.class,
                compilerProperties.jobQueue(),
                messageId
            );
            if (Boolean.FALSE.equals(deleted)) {
                LOGGER.warn("Message {} was not deleted from queue {}", messageId, compilerProperties.jobQueue());
            }
        } catch (DataAccessException ex) {
            LOGGER.error("Failed to delete message {} from queue {}", messageId, compilerProperties.jobQueue(), ex);
        }
    }

    @Override
    public void publishResult(CompilationResult result) {
        try {
            PGobject payload = new PGobject();
            payload.setType("jsonb");
            payload.setValue(objectMapper.writeValueAsString(result));
            jdbcTemplate.queryForObject(
                SEND_SQL,
                Long.class,
                compilerProperties.resultQueue(),
                payload
            );
        } catch (SQLException | JsonProcessingException ex) {
            LOGGER.error("Failed to serialize compilation result for function {}", result.functionId(), ex);
        } catch (DataAccessException ex) {
            LOGGER.error("Failed to publish result for function {}", result.functionId(), ex);
        }
    }

    private QueuedCompilationJob mapQueuedCompilationJob(ResultSet rs, int rowNum) throws SQLException {
        long messageId = rs.getLong("msg_id");
        String payload = rs.getString("message");
        try {
            CompilationJob job = objectMapper.readValue(payload, CompilationJob.class);
            return new QueuedCompilationJob(messageId, job);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize compilation job " + messageId, ex);
        }
    }

    private int visibilityTimeoutSeconds() {
        long timeoutMs = compilerProperties.timeoutMs();
        if (timeoutMs <= 0) {
            return DEFAULT_VT_SECONDS;
        }
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeoutMs);
        if (seconds <= 0) {
            seconds = 1;
        }
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }
}
