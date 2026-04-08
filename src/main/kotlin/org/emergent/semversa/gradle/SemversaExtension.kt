package org.emergent.semversa.gradle

import org.emergent.semversa.Config
import org.emergent.semversa.VersionInfo
import org.emergent.semversa.VersionResolver
import org.emergent.semversa.git.GitRepo
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input

abstract class SemversaExtension(project: Project) {

    @get:Input abstract val releaseBranchRegex: Property<String>
    @get:Input abstract val tagNameRegex: Property<String>
    @get:Input abstract val versionPattern: Property<String>

    /** The root git repository directory. */
    @get:Input abstract val repoRoot: DirectoryProperty

    /** A list of environment variables to read the branch name from. */
    @get:Input abstract val branchEnv: ListProperty<String>

    /** Pattern used to match when looking for the last tag. */
    @get:Input abstract val lastTagPattern: Property<String>

    val info: Provider<VersionInfo> = project.providers.provider {
        resolver().versionInfo()
    }

    private fun resolver(): VersionResolver {
        return VersionResolver(config(), repo())
    }

    private fun repo(): GitRepo {
        return GitRepo(repoRoot.get().asFile)
    }

    private fun config(): Config {
        return Config(
            releaseBranchRegex = releaseBranchRegex.get(),
            tagNameRegex = tagNameRegex.get(),
            versionPattern = versionPattern.get()
        )
    }
}
