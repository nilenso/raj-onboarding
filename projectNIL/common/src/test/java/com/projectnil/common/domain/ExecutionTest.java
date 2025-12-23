package com.projectnil.common.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExecutionTest {

    @Test
    void builderProducesFullyPopulatedExecution() {
        UUID id = UUID.randomUUID();
        UUID functionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Execution execution = Execution.builder()
                .id(id)
                .functionId(functionId)
                .input("{\"value\":1}")
                .output("{\"result\":2}")
                .status(ExecutionStatus.COMPLETED)
                .errorMessage("none")
                .startedAt(now)
                .completedAt(now.plusSeconds(1))
                .build();

        assertEquals(id, execution.getId());
        assertEquals(functionId, execution.getFunctionId());
        assertEquals("{\"value\":1}", execution.getInput());
        assertEquals("{\"result\":2}", execution.getOutput());
        assertEquals(ExecutionStatus.COMPLETED, execution.getStatus());
        assertEquals("none", execution.getErrorMessage());
        assertEquals(now, execution.getStartedAt());
        assertEquals(now.plusSeconds(1), execution.getCompletedAt());
    }

    @Test
    void builderDefaultsToPendingStatus() {
        Execution execution = Execution.builder()
                .functionId(UUID.randomUUID())
                .build();

        assertEquals(ExecutionStatus.PENDING, execution.getStatus());
    }

    @Test
    void protectedConstructorAvailableForJpa() {
        Execution execution = new Execution();
        assertNotNull(execution);
    }
}
