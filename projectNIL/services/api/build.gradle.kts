plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency-management)
}

group = "com.projectnil"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data-jpa)
    implementation(libs.liquibase.core)
    runtimeOnly(libs.postgresql)
    
    // Chicory WASM runtime
    implementation(libs.chicory.core)
    
    testImplementation(libs.spring.boot.starter.test)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.projectnil.api.ApiApplication")
}
