import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.21"
    distribution
    signing
    id("com.gradle.plugin-publish") version "1.3.1"
    id("com.github.breadmoirai.github-release") version "2.2.12"
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
    idea
    id("org.emergent.semversa") version "1.0.0"
}

val bootstrap: String? by project
if (!bootstrap.isNullOrEmpty()) {
    version = bootstrap!!
} else if ("$version" == "unspecified" && project.extra.has("version")) {
    version = project.extra["version"]!!
}

group = "org.emergent.semversa"

val jdkVersion: Int = 17
val jdkTarget: Int = 17

val gitHubOwner: String by project
val gitHubRepo: String by project
val gitHubToken: String by project
val gitHubCommit: String by project

val mvnOrgPath = (project.group as String).replace(".", "/")
val mvnLibraryPath = "${mvnOrgPath}/${project.name}"
val mvnPluginPath = "${mvnOrgPath}/${project.group}.gradle.plugin"

val stagingDir = layout.buildDirectory.dir("staging-deploy")

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(libs.bundles.jgit)
    implementation(libs.commons.lang3)
    implementation(libs.jspecify)
    implementation(libs.semver)
    testImplementation(libs.commons.io)
    testImplementation(libs.assertj.core)
    testImplementation(libs.bundles.junit.impl)
    testRuntimeOnly(libs.bundles.junit.runtime)
}

gradlePlugin {
    website.set("https://github.com/${gitHubOwner}/${gitHubRepo}")
    vcsUrl.set("https://github.com/${gitHubOwner}/${gitHubRepo}")
    vcsUrl.set("git@github.com:${gitHubOwner}/${gitHubRepo}.git")
    plugins {
        create("semversa") {
            id = "org.emergent.semversa"
            version = project.version
            displayName = "Versioning Plugin for Gradle"
            description = "Gradle plug-in that computes version information from the SCM"
            implementationClass = "org.emergent.semversa.gradle.SemversaPlugin"
            tags.set(listOf("gradle", "plugin", "scm", "git", "version", "semver"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "staging"
            url = uri(stagingDir)
        }
        val nexusReleasesUrl: String? by project
        val nexusSnapshotsUrl: String? by project
        if (!bootstrap.isNullOrEmpty() && !nexusSnapshotsUrl.isNullOrEmpty() && !nexusReleasesUrl.isNullOrEmpty()) {
            maven {
                name = "nexus"
                url = uri(if ("$version".endsWith("SNAPSHOT")) nexusSnapshotsUrl!! else nexusReleasesUrl!!)
                credentials(PasswordCredentials::class)
            }
        }
    }
    publications {
        create<MavenPublication>("pluginMaven") {
            pom {
                name.set("Semversa")
                description.set("A library for computing version information from the SCM")
            }
        }
        matching { it is MavenPublication }.configureEach { this as MavenPublication
            pom {
                url.set("https://github.com/${gitHubOwner}/${gitHubRepo}")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/MIT")
                    }
                }
                developers {
                    developer {
                        name.set("Patrick Woodworth")
                        email.set("patrick@woodworth.org")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/${gitHubOwner}/${gitHubRepo}")
                    developerConnection.set("scm:git@github.com:${gitHubOwner}/${gitHubRepo}.git")
                    url.set("https://github.com/${gitHubOwner}/${gitHubRepo}")
                }
            }
        }
    }
}

distributions {
    main {
        contents {
            into("/") {
                from(stagingDir)
            }
        }
    }
}

dokka {
    moduleName.set("Versioning Plugin for Gradle")
    dokkaPublications.javadoc {
    }
    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/${gitHubOwner}/${gitHubRepo}")
            remoteLineSuffix.set("#L")
        }
    }
}

tasks.test {
    useJUnitPlatform()
    environment("GIT_TEST_BRANCH", "feature/456-cute")
    environment("SVN_TEST_BRANCH", "feature-456-cute")
}

githubRelease {
    token(gitHubToken)
    owner(gitHubOwner)
    repo(gitHubRepo)
    tagName(version.toString())
    releaseName(version.toString())
    targetCommitish(gitHubCommit)
    overwrite(true)
}

signing {
    val signingKeys: String? by project
    val signingPass: String? by project
    useInMemoryPgpKeys(signingKeys, signingPass)
    sign(publishing.publications)
}

idea {
    module {
        isDownloadSources = true
    }
}

kotlin {
    jvmToolchain(jdkVersion)
    compilerOptions.jvmTarget = JvmTarget.valueOf("JVM_${jdkTarget}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jdkVersion)
    }
}

tasks.withType<JavaCompile> {
    options.release = jdkTarget
    options.compilerArgs.add("-Xlint:all,-processing,-serial")
}

tasks.withType<Javadoc> {
    isFailOnError = false
    options {
        this as StandardJavadocDocletOptions
        addBooleanOption("Xdoclint:none", true)
        addBooleanOption("quiet", true)
    }
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Automatic-Module-Name" to "org.emergent.semversa.gradle",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        ))
    }
    into("META-INF/maven/${project.group}/${project.name}") {
        from(tasks["generatePomFileForPluginMavenPublication"])
            .rename { it.replace("pom-default.xml", "pom.xml") }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.javadoc, tasks.dokkaGeneratePublicationJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
    from(tasks.dokkaGeneratePublicationJavadoc)
}

tasks.matching { it.name.startsWith("dist") }.configureEach {
    dependsOn(tasks["publishAllPublicationsToStagingRepository"])
}

tasks.withType<Sign>().configureEach {
    onlyIf("signingKeys is set") { project.hasProperty("signingKeys") }
}
