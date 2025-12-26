package com.projectnil.compiler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.compiler.messaging.JdbcPgmqClient;
import com.projectnil.compiler.messaging.PgmqClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties({CompilerProperties.class, PgmqProperties.class})
public class CompilerMessagingConfiguration {

    @Bean
    public PgmqClient pgmqClient(
        JdbcTemplate jdbcTemplate,
        CompilerProperties compilerProperties,
        ObjectMapper objectMapper
    ) {
        return new JdbcPgmqClient(jdbcTemplate, compilerProperties, objectMapper);
    }
}
