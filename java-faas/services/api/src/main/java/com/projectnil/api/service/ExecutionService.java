package com.projectnil.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.api.repository.ExecutionRepository;
import com.projectnil.api.runtime.WasmExecutionException;
import com.projectnil.api.runtime.WasmRuntime;
import com.projectnil.api.web.ExecutionDetailResponse;
import com.projectnil.api.web.ExecutionRequest;
import com.projectnil.api.web.ExecutionResponse;
import com.projectnil.api.web.ExecutionSummaryResponse;
import com.projectnil.common.domain.Execution;
import com.projectnil.common.domain.ExecutionStatus;
import com.projectnil.common.domain.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for executing functions.
 * Orchestrates the WASM runtime execution and persists execution records.
 */
@Service
public class ExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionService.class);

    private final FunctionService functionService;
    private final ExecutionRepository executionRepository;
    private final WasmRuntime wasmRuntime;
    private final ObjectMapper objectMapper;

    public ExecutionService(
            FunctionService functionService,
            ExecutionRepository executionRepository,
            WasmRuntime wasmRuntime,
            ObjectMapper objectMapper) {
        this.functionService = functionService;
        this.executionRepository = executionRepository;
        this.wasmRuntime = wasmRuntime;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute a function with the given input.
     *
     * <p>Flow per scope/flows.md Flow 3:
     * <ol>
     *   <li>Validate function exists and is READY</li>
     *   <li>Create Execution record with RUNNING status</li>
     *   <li>Execute WASM via WasmRuntime</li>
     *   <li>Update Execution with result (COMPLETED or FAILED)</li>
     * </ol>
     *
     * @param functionId the function ID
     * @param request the execution request containing input
     * @return the execution response
     * @throws FunctionNotFoundException if function not found
     * @throws FunctionNotReadyException if function not in READY status
     */
    @Transactional
    public ExecutionResponse execute(UUID functionId, ExecutionRequest request) {
        LOG.info("execution.started functionId={}", functionId);

        // Validate function exists and is READY (throws if not)
        Function function = functionService.findReadyById(functionId);

        // Serialize input to JSON string for storage and WASM
        String inputJson = serializeInput(request.input());

        // Create execution record with RUNNING status
        Execution execution = Execution.builder()
                .functionId(functionId)
                .input(inputJson)
                .status(ExecutionStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        execution = executionRepository.save(execution);

        try {
            // Execute WASM
            byte[] outputBytes = wasmRuntime.execute(function.getWasmBinary(), inputJson);
            String outputJson = new String(outputBytes, StandardCharsets.UTF_8);

            // Update execution as COMPLETED
            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setOutput(outputJson);
            execution.setCompletedAt(LocalDateTime.now());
            execution = executionRepository.save(execution);

            LOG.info("execution.completed executionId={} functionId={}",
                    execution.getId(), functionId);

            return toResponse(execution);

        } catch (WasmExecutionException e) {
            // User code error (trap, timeout) - mark as FAILED but return 200
            LOG.warn("execution.failed executionId={} functionId={} error={}",
                    execution.getId(), functionId, e.getMessage());

            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution = executionRepository.save(execution);

            return toResponse(execution);

        } catch (Exception e) {
            // Unexpected error - still mark execution as FAILED
            LOG.error("execution.failed executionId={} functionId={} unexpected error",
                    execution.getId(), functionId, e);

            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage("Internal error: " + e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution = executionRepository.save(execution);

            return toResponse(execution);
        }
    }

    /**
     * Find an execution by ID (for execute response).
     *
     * @param executionId the execution ID
     * @return the execution response
     * @throws ExecutionNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public ExecutionResponse findById(UUID executionId) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException(executionId));
        return toResponse(execution);
    }

    /**
     * Get detailed execution by ID.
     *
     * <p>Per issue #30, returns all fields including input, output, timestamps.
     *
     * @param executionId the execution ID
     * @return the detailed execution response
     * @throws ExecutionNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public ExecutionDetailResponse getById(UUID executionId) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException(executionId));
        return toDetailResponse(execution);
    }

    /**
     * Find all executions for a function.
     *
     * <p>Per issue #31, returns lightweight summaries ordered by startedAt DESC.
     * Validates that the function exists first.
     *
     * @param functionId the function ID
     * @return list of execution summaries
     * @throws FunctionNotFoundException if function not found
     */
    @Transactional(readOnly = true)
    public List<ExecutionSummaryResponse> findByFunctionId(UUID functionId) {
        // Validate function exists (throws 404 if not)
        functionService.findById(functionId);

        return executionRepository.findByFunctionIdOrderByCreatedAtDesc(functionId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Validate and serialize input to JSON string.
     *
     * <p>Per scope/contracts.md, input must be a JSON object (not primitive, array, or null).
     */
    private String serializeInput(Object input) {
        if (input == null) {
            return "{}";
        }
        // Validate input is a JSON object (Map), not primitive or array
        if (!(input instanceof java.util.Map)) {
            throw new InvalidInputException(
                    "Input must be a JSON object, got: " + input.getClass().getSimpleName());
        }
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException("Failed to serialize input to JSON: " + e.getMessage());
        }
    }

    private ExecutionResponse toResponse(Execution execution) {
        return new ExecutionResponse(
                execution.getId(),
                execution.getFunctionId(),
                execution.getStatus(),
                parseOutput(execution.getOutput()),
                execution.getErrorMessage(),
                execution.getCreatedAt()
        );
    }

    /**
     * Parse output JSON string into an Object for response serialization.
     * Per scope/contracts.md, output should be returned as parsed JSON object.
     */
    private Object parseOutput(String outputJson) {
        if (outputJson == null || outputJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(outputJson, Object.class);
        } catch (JsonProcessingException e) {
            // If parsing fails, return as raw string (graceful degradation)
            LOG.warn("Failed to parse output JSON, returning as string: {}", e.getMessage());
            return outputJson;
        }
    }

    private ExecutionDetailResponse toDetailResponse(Execution execution) {
        return new ExecutionDetailResponse(
                execution.getId(),
                execution.getFunctionId(),
                execution.getStatus(),
                parseOutput(execution.getInput()),
                parseOutput(execution.getOutput()),
                execution.getErrorMessage(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getCreatedAt()
        );
    }

    private ExecutionSummaryResponse toSummaryResponse(Execution execution) {
        return new ExecutionSummaryResponse(
                execution.getId(),
                execution.getStatus(),
                execution.getStartedAt(),
                execution.getCompletedAt()
        );
    }
}
