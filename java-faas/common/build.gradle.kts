group = "com.projectnil"
version = "0.0.1-SNAPSHOT"

plugins {
        id("java")
        alias(libs.plugins.spring.dependency.management)
}

dependencies {
        implementation(libs.spring.boot.starter.data.jpa)
        compileOnly(libs.lombok)
        annotationProcessor(libs.lombok)

        testCompileOnly(libs.lombok)
        testAnnotationProcessor(libs.lombok)
        testRuntimeOnly(libs.junit.platform.launcher)
}
