package com.projectnil.compiler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectnil.compiler.core.AssemblyScriptCompiler;
import com.projectnil.compiler.core.CompilerRunner;
import com.projectnil.compiler.core.DefaultCompilerRunner;
import com.projectnil.compiler.core.FileSystemWorkspaceManager;
import com.projectnil.compiler.core.LanguageCompiler;
import com.projectnil.compiler.core.ProcessExecutor;
import com.projectnil.compiler.core.WorkspaceManager;
import com.projectnil.compiler.messaging.JdbcPgmqClient;
import com.projectnil.compiler.messaging.PgmqClient;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties({CompilerProperties.class, PgmqProperties.class})
public class CompilerMessagingConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public PgmqClient pgmqClient(
        JdbcTemplate jdbcTemplate,
        CompilerProperties compilerProperties,
        ObjectMapper objectMapper
    ) {
        return new JdbcPgmqClient(jdbcTemplate, compilerProperties, objectMapper);
    }

    @Bean
    public WorkspaceManager workspaceManager(CompilerProperties compilerProperties) {
        return new FileSystemWorkspaceManager(Path.of(compilerProperties.workspaceDir()));
    }

    @Bean
    public ProcessExecutor processExecutor() {
        return new ProcessExecutor();
    }

    @Bean
    public LanguageCompiler languageCompiler(
        WorkspaceManager workspaceManager,
        ProcessExecutor processExecutor,
        CompilerProperties compilerProperties
    ) {
        Duration timeout = Duration.ofMillis(compilerProperties.timeoutMs());
        return new AssemblyScriptCompiler(
            workspaceManager,
            processExecutor,
            timeout,
            compilerProperties.ascBinary()
        );
    }

    @Bean
    public CompilerRunner compilerRunner(
        PgmqClient pgmqClient,
        LanguageCompiler languageCompiler,
        CompilerProperties compilerProperties
    ) {
        return new DefaultCompilerRunner(pgmqClient, languageCompiler, compilerProperties);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startCompilerRunner(ApplicationReadyEvent event) {
        CompilerRunner runner = event.getApplicationContext().getBean(CompilerRunner.class);
        runner.start();
    }
}
