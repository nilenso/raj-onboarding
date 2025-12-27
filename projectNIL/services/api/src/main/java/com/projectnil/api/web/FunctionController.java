package com.projectnil.api.web;

import com.projectnil.api.service.ExecutionService;
import com.projectnil.api.service.FunctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for function operations.
 */
@RestController
@RequestMapping("/functions")
public class FunctionController {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionController.class);

    private final FunctionService functionService;
    private final ExecutionService executionService;

    public FunctionController(FunctionService functionService, ExecutionService executionService) {
        this.functionService = functionService;
        this.executionService = executionService;
    }

    /**
     * Create a new function.
     *
     * <p>Per scope/flows.md Flow 1:
     * <ul>
     *   <li>Validates request and language support</li>
     *   <li>Creates function with PENDING status</li>
     *   <li>Publishes compilation job to queue</li>
     *   <li>Returns 201 with FunctionResponse</li>
     * </ul>
     *
     * @param request the function creation request
     * @return the created function response
     */
    @PostMapping
    public ResponseEntity<FunctionResponse> create(@RequestBody FunctionRequest request) {
        LOG.debug("Received create function request: name={}", request.name());

        FunctionResponse response = functionService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all functions.
     *
     * @return list of all functions
     */
    @GetMapping
    public ResponseEntity<List<FunctionResponse>> list() {
        return ResponseEntity.ok(functionService.findAll());
    }

    /**
     * Get a function by ID.
     *
     * @param functionId the function ID
     * @return the function details
     */
    @GetMapping("/{functionId}")
    public ResponseEntity<FunctionResponse> get(@PathVariable UUID functionId) {
        var function = functionService.findById(functionId);
        return ResponseEntity.ok(new FunctionResponse(
                function.getId(),
                function.getName(),
                function.getStatus(),
                function.getCreatedAt()
        ));
    }

    /**
     * Update a function.
     *
     * <p>Per scope/contracts.md and issue #27:
     * <ul>
     *   <li>Updates name, description, language, source</li>
     *   <li>If source or language changes, triggers recompilation</li>
     *   <li>Returns expanded view with all fields</li>
     * </ul>
     *
     * @param functionId the function ID
     * @param request the update request
     * @return the updated function (expanded view)
     */
    @PutMapping("/{functionId}")
    public ResponseEntity<FunctionDetailResponse> update(
            @PathVariable UUID functionId,
            @RequestBody FunctionRequest request) {
        LOG.debug("Received update function request: id={}", functionId);

        FunctionDetailResponse response = functionService.update(functionId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a function by ID.
     *
     * @param functionId the function ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{functionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID functionId) {
        functionService.delete(functionId);
        return ResponseEntity.noContent().build();
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
