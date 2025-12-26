package com.projectnil.api.web;

import com.projectnil.api.runtime.WasmAbiException;
import com.projectnil.api.service.ExecutionNotFoundException;
import com.projectnil.api.service.FunctionNotFoundException;
import com.projectnil.api.service.FunctionNotReadyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler for REST API.
 *
 * <p>Error semantics per scope/contracts.md:
 * <ul>
 *   <li>400 Bad Request - Invalid DTO payload, attempt to execute non-READY function</li>
 *   <li>404 Not Found - Unknown function/execution ID</li>
 *   <li>500 Internal Server Error - Unexpected platform failure</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FunctionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFunctionNotFound(FunctionNotFoundException ex) {
        LOG.warn("Function not found: {}", ex.getFunctionId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ExecutionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleExecutionNotFound(ExecutionNotFoundException ex) {
        LOG.warn("Execution not found: {}", ex.getExecutionId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(FunctionNotReadyException.class)
    public ResponseEntity<Map<String, Object>> handleFunctionNotReady(FunctionNotReadyException ex) {
        LOG.warn("Function not ready: {} (status={})", ex.getFunctionId(), ex.getCurrentStatus());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(WasmAbiException.class)
    public ResponseEntity<Map<String, Object>> handleWasmAbiException(WasmAbiException ex) {
        // ABI violations are platform errors (compiled WASM is malformed)
        LOG.error("WASM ABI violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "WASM ABI violation: " + ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        LOG.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        LOG.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
