package com.projectnil.api.web;

import com.projectnil.api.service.ExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for execution operations.
 *
 * <p>Provides endpoints for querying execution details per issue #30.
 */
@RestController
@RequestMapping("/executions")
public class ExecutionController {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionController.class);

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * Get execution details by ID.
     *
     * <p>Per scope/contracts.md and issue #30:
     * <ul>
     *   <li>Returns detailed execution record including input, output, timestamps</li>
     *   <li>Returns 404 if execution does not exist</li>
     *   <li>errorMessage is included only for FAILED executions</li>
     * </ul>
     *
     * @param executionId the execution ID
     * @return the execution details
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionDetailResponse> get(@PathVariable UUID executionId) {
        LOG.debug("Received get execution request: id={}", executionId);

        ExecutionDetailResponse response = executionService.getById(executionId);

        return ResponseEntity.ok(response);
    }
}
