group = "com.projectnil"
version = "0.0.1-SNAPSHOT"

plugins {
        id("java")
        alias(libs.plugins.spring.dependency.management)
}

dependencies {
        implementation(libs.spring.boot.starter.data.jpa)
}
