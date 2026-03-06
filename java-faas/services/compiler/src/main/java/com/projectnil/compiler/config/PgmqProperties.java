package com.projectnil.compiler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pgmq")
public record PgmqProperties(
    String url,
    String username,
    String password
) {}
