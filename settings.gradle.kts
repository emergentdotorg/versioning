pluginManagement {
    repositories {
        mavenLocal()
        val nexusReleasesUrl: String? by settings
        if (!nexusReleasesUrl.isNullOrEmpty()) {
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
