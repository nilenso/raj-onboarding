package com.projectnil.api.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for PGMQ messaging components.
 */
@Configuration
@EnableConfigurationProperties(PgmqProperties.class)
public class PgmqConfiguration {

    @Bean
    public PgmqClient pgmqClient(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PgmqProperties properties) {
        return new JdbcPgmqClient(
                jdbcTemplate,
                objectMapper,
                properties.jobQueue(),
                properties.resultQueue()
        );
    }
}
