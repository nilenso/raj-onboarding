package com.projectnil.api.runtime;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the WASM runtime.
 * 
 * <p>Configure via application.yaml:
 * <pre>
 * projectnil:
 *   wasm:
 *     timeout: 10s
 * </pre>
 * 
 * @param timeout Maximum execution time for WASM functions. Default: 10 seconds.
 */
@ConfigurationProperties(prefix = "projectnil.wasm")
public record WasmRuntimeProperties(
    Duration timeout
) {
    /**
     * Default timeout of 10 seconds.
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public WasmRuntimeProperties {
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT;
        }
    }
}
