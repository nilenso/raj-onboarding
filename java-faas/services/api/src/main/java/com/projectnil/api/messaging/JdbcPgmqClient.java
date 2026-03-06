package com.projectnil.api.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.common.domain.queue.CompilationJob;
import com.projectnil.common.domain.queue.CompilationResult;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based implementation of {@link PgmqClient}.
 *
 * <p>Uses pgmq SQL functions to interact with queues:
 * <ul>
 *   <li>{@code pgmq.send(queue, message)} - publish message</li>
 *   <li>{@code pgmq.read(queue, vt, limit)} - read messages with visibility timeout</li>
 *   <li>{@code pgmq.delete(queue, msgId)} - delete processed message</li>
 * </ul>
 */
public class JdbcPgmqClient implements PgmqClient {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcPgmqClient.class);

    private static final String SEND_SQL = "SELECT pgmq.send(?, ?)";
    private static final String READ_SQL = "SELECT msg_id, message FROM pgmq.read(?, ?, 1)";
    private static final String DELETE_SQL = "SELECT pgmq.delete(?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String jobQueue;
    private final String resultQueue;

    public JdbcPgmqClient(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            String jobQueue,
            String resultQueue) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.jobQueue = jobQueue;
        this.resultQueue = resultQueue;
    }

    @Override
    public long publishJob(CompilationJob job) {
        try {
            PGobject payload = new PGobject();
            payload.setType("jsonb");
            payload.setValue(objectMapper.writeValueAsString(job));

            Long messageId = jdbcTemplate.queryForObject(SEND_SQL, Long.class, jobQueue, payload);
            if (messageId == null) {
                throw new IllegalStateException("pgmq.send returned null for job " + job.functionId());
            }

            LOG.info("compilation.job.published functionId={} messageId={}", job.functionId(), messageId);
            return messageId;

        } catch (SQLException | JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize compilation job for " + job.functionId(), ex);
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Failed to publish job for " + job.functionId(), ex);
        }
    }

    @Override
    public Optional<QueuedCompilationResult> readResult(int visibilityTimeoutSeconds) {
        try {
            List<QueuedCompilationResult> results = jdbcTemplate.query(
                    READ_SQL,
                    ps -> {
                        ps.setString(1, resultQueue);
                        ps.setInt(2, visibilityTimeoutSeconds);
                    },
                    this::mapQueuedCompilationResult
            );
            return results.stream().findFirst();
        } catch (DataAccessException ex) {
            LOG.error("Failed to read result from queue {}", resultQueue, ex);
            return Optional.empty();
        }
    }

    @Override
    public void deleteResult(long messageId) {
        try {
            Boolean deleted = jdbcTemplate.queryForObject(DELETE_SQL, Boolean.class, resultQueue, messageId);
            if (Boolean.FALSE.equals(deleted)) {
                LOG.warn("Message {} was not deleted from queue {}", messageId, resultQueue);
            }
        } catch (DataAccessException ex) {
            LOG.error("Failed to delete message {} from queue {}", messageId, resultQueue, ex);
        }
    }

    private QueuedCompilationResult mapQueuedCompilationResult(ResultSet rs, int rowNum) throws SQLException {
        long messageId = rs.getLong("msg_id");
        String payload = rs.getString("message");
        try {
            // Parse the JSON payload
            var node = objectMapper.readTree(payload);

            var functionId = java.util.UUID.fromString(node.get("functionId").asText());
            boolean success = node.get("success").asBoolean();

            byte[] wasmBinary = null;
            if (node.has("wasmBinary") && !node.get("wasmBinary").isNull()) {
                String base64 = node.get("wasmBinary").asText();
                wasmBinary = Base64.getDecoder().decode(base64);
            }

            String error = null;
            if (node.has("error") && !node.get("error").isNull()) {
                error = node.get("error").asText();
            }

            CompilationResult result = new CompilationResult(functionId, success, wasmBinary, error);
            return new QueuedCompilationResult(messageId, result);

        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize compilation result " + messageId, ex);
        }
    }
}
