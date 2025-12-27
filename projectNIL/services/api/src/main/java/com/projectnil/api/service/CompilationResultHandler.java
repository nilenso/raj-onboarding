package com.projectnil.api.service;

import com.projectnil.api.repository.FunctionRepository;
import com.projectnil.common.domain.Function;
import com.projectnil.common.domain.FunctionStatus;
import com.projectnil.common.domain.queue.CompilationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles compilation results by updating function status.
 *
 * <p>Per Issue #53 and scope/practices.md, this handler is idempotent:
 * <ul>
 *   <li>Re-applying the same result does not corrupt state</li>
 *   <li>If function is already READY or FAILED, the result is skipped</li>
 * </ul>
 */
@Service
public class CompilationResultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CompilationResultHandler.class);

    private final FunctionRepository functionRepository;

    public CompilationResultHandler(FunctionRepository functionRepository) {
        this.functionRepository = functionRepository;
    }

    /**
     * Apply a compilation result to the corresponding function.
     *
     * @param result the compilation result
     * @return true if the result was applied, false if skipped
     */
    @Transactional
    public boolean applyResult(CompilationResult result) {
        var functionId = result.functionId();

        var optionalFunction = functionRepository.findById(functionId);
        if (optionalFunction.isEmpty()) {
            LOG.warn("Function not found for compilation result: {}", functionId);
            return false;
        }

        Function function = optionalFunction.get();

        // Idempotency check: skip if already in terminal state
        if (function.getStatus() == FunctionStatus.READY
                || function.getStatus() == FunctionStatus.FAILED) {
            LOG.debug("Function {} already in terminal state {}, skipping result",
                    functionId, function.getStatus());
            return false;
        }

        if (result.success()) {
            applySuccessResult(function, result);
        } else {
            applyFailureResult(function, result);
        }

        functionRepository.save(function);
        return true;
    }

    private void applySuccessResult(Function function, CompilationResult result) {
        function.setStatus(FunctionStatus.READY);
        function.setWasmBinary(result.wasmBinary());
        function.setCompileError(null);

        LOG.info("Function {} compiled successfully, status=READY", result.functionId());
    }

    private void applyFailureResult(Function function, CompilationResult result) {
        function.setStatus(FunctionStatus.FAILED);
        function.setWasmBinary(null);
        function.setCompileError(result.error());

        LOG.info("Function {} compilation failed, status=FAILED, error={}",
                result.functionId(), result.error());
    }
}
