pluginManagement {
    repositories {
        mavenLocal()
        val bootstrap: String? by settings
        val nexusReleasesUrl: String? by settings
        if (!bootstrap.isNullOrEmpty() && !nexusReleasesUrl.isNullOrEmpty()) {
            maven {
                name = "nexusReadOnly"
                url = uri(nexusReleasesUrl!!)
                credentials(PasswordCredentials::class)
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "semversa-gradle-plugin"
