import org.gradle.api.tasks.testing.Test
import org.gradle.api.plugins.JavaPluginExtension

plugins {
    id("base")
}

val checkstyleVersion = libs.versions.checkstyle.get()

val junitLibrary = libs.junit.jupiter

subprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
            mavenCentral()
    }

    configure<CheckstyleExtension> {
        toolVersion = checkstyleVersion
        isIgnoreFailures = false
        isShowViolations = true
    }

    dependencies {
        "testImplementation"(junitLibrary)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
