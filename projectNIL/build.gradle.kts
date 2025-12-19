import org.gradle.api.tasks.testing.Test
import org.gradle.api.plugins.JavaPluginExtension

plugins {
    id("base")
}

val checkstyleVersion = libs.versions.checkstyle.get()

// Note: Ensure your TOML has 'junit-jupiter' (aggregator) or use 'libs.junit.jupiter.api' 
// based on your provided TOML. I'm assuming you want the aggregator.
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
        // 2. FIX: Use string syntax for dynamic plugins
        // Instead of testImplementation(...), use "testImplementation"(...)
        "testImplementation"(junitLibrary)
    }

    // 3. FIX: Use 'withType' or 'named' to configure the task
    // Instead of tasks.test { }, use this:
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
