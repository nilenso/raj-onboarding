package com.projectnil.api.web;

import com.projectnil.api.service.ExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for function operations.
 */
@RestController
@RequestMapping("/functions")
public class FunctionController {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionController.class);

    private final ExecutionService executionService;

    public FunctionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * Execute a function with the given input.
     *
     * <p>Per scope/contracts.md and scope/practices.md:
     * <ul>
     *   <li>Returns 200 with ExecutionResponse even when execution fails (user code error)</li>
     *   <li>Returns 400 when function is not in READY status</li>
     *   <li>Returns 404 when function does not exist</li>
     * </ul>
     *
     * @param functionId the function ID
     * @param request the execution request
     * @return the execution response
     */
    @PostMapping("/{functionId}/execute")
    public ResponseEntity<ExecutionResponse> execute(
            @PathVariable UUID functionId,
            @RequestBody ExecutionRequest request) {

        LOG.debug("Received execute request for function {}", functionId);

        ExecutionResponse response = executionService.execute(functionId, request);

        return ResponseEntity.ok(response);
    }
}
