plugins{
    id("base")
}


val checkstyleVersion = libs.versions.checkstyle.get()

subprojects{

    apply(plugin = "checkstyle")

    configure<CheckstyleExtension>{
        toolVersion = checkstyleVersion
        isIgnoreFailures = false
        isShowViolations = true
    }
}
