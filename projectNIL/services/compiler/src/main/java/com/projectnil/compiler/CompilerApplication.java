package com.projectnil.compiler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CompilerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompilerApplication.class, args);
    }
}
