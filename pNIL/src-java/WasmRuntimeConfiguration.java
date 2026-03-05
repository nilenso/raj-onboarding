package com.projectnil.api.runtime;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for WASM runtime beans.
 */
@Configuration
@EnableConfigurationProperties(WasmRuntimeProperties.class)
public class WasmRuntimeConfiguration {

    /**
     * Creates the string codec for AssemblyScript modules.
     * 
     * <p>Future: This could be made language-aware by injecting Function.language
     * and selecting the appropriate codec.
     */
    @Bean
    public WasmStringCodec wasmStringCodec() {
        return new AssemblyScriptStringCodec();
    }

    /**
     * Creates the WASM runtime using Chicory.
     */
    @Bean
    public WasmRuntime wasmRuntime(
            WasmStringCodec stringCodec,
            WasmRuntimeProperties properties) {
        return new ChicoryWasmRuntime(stringCodec, properties.timeout());
    }
}
